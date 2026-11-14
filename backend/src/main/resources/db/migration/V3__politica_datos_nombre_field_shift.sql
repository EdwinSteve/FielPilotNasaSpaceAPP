-- Versión 1.1 de la política de datos: la app se llama Field Shift (antes SURCO). El contenido no cambia.
-- No se edita V2: ya está aplicada en otros ambientes. Los usuarios registrados conservan el
-- consentimiento de la 1.0; desde ahora el registro pide aceptar la 1.1.
-- PENDIENTE: revisión legal antes de publicar.
update identidad.politicas_datos set vigente = false where vigente;

insert into identidad.politicas_datos (version, url_texto, contenido, vigente) values (
'1.1',
'/politica-de-datos',
'Field Shift trata sus datos personales según la Ley 1581 de 2012 de Colombia y sus decretos reglamentarios.

Qué datos recogemos: su nombre, su número de celular, su correo electrónico si decide darlo, su municipio y la ubicación de los lotes que registre.

Para qué los usamos: únicamente para darle recomendaciones agroclimáticas de su finca y avisarle de pronósticos de lluvia y sequía. No vendemos sus datos ni los compartimos con terceros para publicidad.

Cómo los protegemos: su contraseña se guarda cifrada y nadie del equipo puede verla. El acceso a sus datos exige iniciar sesión.

Sus derechos: puede conocer, actualizar, corregir o pedir que borremos sus datos en cualquier momento, y revocar esta autorización.',
true);
