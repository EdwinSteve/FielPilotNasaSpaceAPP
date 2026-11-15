<#
.SYNOPSIS
  Entrena el planificador estacional de SURCO en la máquina Windows con la RTX 5060 Ti.

.DESCRIPTION
  Doble clic en entrenar_windows.bat (o este comando desde la raíz del repositorio):

    powershell -NoProfile -ExecutionPolicy Bypass -File ml\planificador\entrenar_windows.ps1

  1. Muestra la GPU y el driver.
  2. Si falta %USERPROFILE%\.cdsapirc, pide la clave del Climate Data Store y lo crea.
  3. Instala uv si falta y crea el entorno desde uv.lock (PyTorch 2.11 CUDA 12.8, ONNX, cdsapi...).
  4. Corre las pruebas, incluida la tubería completa con datos sintéticos.
  5. Mantiene Windows despierto mientras corre (no cambia ninguna configuración del sistema).
  6. Corre todas las etapas. Cada una se salta si ya terminó: tras un corte de luz, un error de
     red o una ventana cerrada, vuelva a correr el mismo comando y sigue donde iba.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File ml\planificador\entrenar_windows.ps1 -Etapas entrenar,evaluar -Forzar entrenar
#>
param(
    [string[]]$Etapas = @("todo"),
    [string[]]$Forzar = @(),
    [string]$Corrida = "principal"
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot
$env:PYTHONUTF8 = "1"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Paso([string]$Mensaje) { Write-Host "`n==> $Mensaje" -ForegroundColor Cyan }

Paso "Driver NVIDIA"
if (Get-Command nvidia-smi -ErrorAction SilentlyContinue) {
    & nvidia-smi --query-gpu=name,driver_version,memory.total --format=csv
} else {
    throw "No está nvidia-smi: instale el driver NVIDIA 570 o más nuevo (serie RTX 50) y repita."
}

Paso "Credenciales del Climate Data Store"
$cdsapirc = Join-Path $env:USERPROFILE ".cdsapirc"
if (-not (Test-Path $cdsapirc)) {
    Write-Host "No existe $cdsapirc. Copie su clave (Personal Access Token) de https://cds.climate.copernicus.eu/profile"
    $clave = (Read-Host "Clave del CDS").Trim()
    if (-not $clave) { throw "Sin clave no se pueden descargar los pronósticos SEAS5." }
    $texto = "url: https://cds.climate.copernicus.eu/api`nkey: $clave`n"
    [System.IO.File]::WriteAllText($cdsapirc, $texto, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Guardado en $cdsapirc (fuera del repositorio)."
}
Write-Host "Recuerde: la licencia del dataset debe estar aceptada en"
Write-Host "https://cds.climate.copernicus.eu/datasets/seasonal-original-single-levels?tab=download (al final)."

Paso "uv"
if (-not (Get-Command uv -ErrorAction SilentlyContinue)) {
    powershell -NoProfile -ExecutionPolicy Bypass -Command "irm https://astral.sh/uv/install.ps1 | iex"
    $env:Path = "$env:USERPROFILE\.local\bin;$env:Path"
}
uv --version

Paso "Entorno (la primera vez descarga ~4 GB de PyTorch CUDA 12.8)"
uv sync --locked --extra datos --extra entrenamiento --extra mongo
if ($LASTEXITCODE -ne 0) { throw "uv sync falló" }

Paso "Pruebas"
uv run --locked pytest -q
if ($LASTEXITCODE -ne 0) { throw "fallaron las pruebas: no entrene sobre una tubería rota" }

Add-Type -Namespace Surco -Name Energia -MemberDefinition @"
[System.Runtime.InteropServices.DllImport("kernel32.dll")]
public static extern uint SetThreadExecutionState(uint flags);
"@
$CONTINUO = [uint32]"0x80000000"
$SISTEMA = [uint32]"0x00000001"

Paso "Tubería: $($Etapas -join ', ')"
[Surco.Energia]::SetThreadExecutionState($CONTINUO -bor $SISTEMA) | Out-Null
try {
    $argumentos = @($Etapas) + @("--corrida", $Corrida)
    if ($Forzar.Count -gt 0) { $argumentos += @("--forzar") + $Forzar }
    uv run --locked python -m surco_planificador @argumentos
    if ($LASTEXITCODE -ne 0) { throw "la tubería se detuvo ($LASTEXITCODE); lea el mensaje de arriba y vuelva a correr el script para continuar" }
} finally {
    [Surco.Energia]::SetThreadExecutionState($CONTINUO) | Out-Null
}

Paso "Listo"
Write-Host "Métricas: ..\..\data\planificador\corridas\$Corrida\metricas.json"
Write-Host "Modelo:   modelos\planificador-<fecha>\ (súbalo al repositorio con su MODELO.md)"
