import { describe, expect, it } from "vitest";
import { celularValido, contrasenaSegura, MENSAJES, sinErrores, validarIngreso, validarRegistro } from "./validaciones";

const registroValido = {
  nombre_completo: "Ernesto Ramírez",
  celular: "3001234567",
  correo: "",
  contrasena: "MiFinca2026",
  departamento_residencia: "41",
  municipio_residencia: "41001",
  acepta_politica: true,
};

describe("celularValido", () => {
  it("acepta un móvil colombiano de 10 dígitos que empieza por 3", () => {
    expect(celularValido("3001234567")).toBe(true);
  });

  it.each(["300123456", "30012345678", "6001234567", "300-123-4567", ""])("rechaza %s", (celular) => {
    expect(celularValido(celular)).toBe(false);
  });
});

describe("contrasenaSegura", () => {
  it("pide mínimo 8 caracteres con letras y números", () => {
    expect(contrasenaSegura("MiFinca2026")).toBe(true);
    expect(contrasenaSegura("corta1")).toBe(false);
    expect(contrasenaSegura("solamenteletras")).toBe(false);
    expect(contrasenaSegura("1234567890")).toBe(false);
  });
});

describe("validarIngreso", () => {
  it("no da errores con celular y contraseña", () => {
    expect(sinErrores(validarIngreso({ celular: "3001234567", contrasena: "x" }))).toBe(true);
  });

  it("marca cada campo que falta", () => {
    expect(validarIngreso({ celular: "", contrasena: "" })).toEqual({
      celular: MENSAJES.celular,
      contrasena: MENSAJES.contrasenaVacia,
    });
  });
});

describe("validarRegistro", () => {
  it("acepta un registro completo con correo vacío", () => {
    expect(validarRegistro(registroValido)).toEqual({});
  });

  it("valida el correo solo si se escribió", () => {
    expect(validarRegistro({ ...registroValido, correo: "no-es-correo" }).correo).toBe(MENSAJES.correo);
  });

  it("exige aceptar la política y elegir municipio", () => {
    const errores = validarRegistro({ ...registroValido, acepta_politica: false, municipio_residencia: "" });
    expect(errores.acepta_politica).toBe(MENSAJES.politica);
    expect(errores.municipio_residencia).toBe(MENSAJES.municipio);
  });

  it("sin departamento pide el departamento, no el municipio", () => {
    const errores = validarRegistro({ ...registroValido, departamento_residencia: "", municipio_residencia: "" });
    expect(errores.departamento_residencia).toBe(MENSAJES.departamento);
    expect(errores.municipio_residencia).toBeUndefined();
  });

  it("ignora los espacios al validar el nombre", () => {
    expect(validarRegistro({ ...registroValido, nombre_completo: "  Al  " }).nombre_completo).toBe(MENSAJES.nombre);
  });
});
