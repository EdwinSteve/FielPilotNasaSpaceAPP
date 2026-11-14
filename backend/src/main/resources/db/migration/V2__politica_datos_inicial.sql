-- Primera versión de la política de tratamiento de datos (Ley 1581 de 2012).
-- PENDIENTE: revisión legal antes de publicar. Para una versión nueva, cree otra migración que
-- inserte la nueva fila y deje vigente = false en la anterior: los usuarios registrados conservan
-- el consentimiento de la versión que aceptaron.
insert into identidad.politicas_datos (version, url_texto, contenido, vigente) values (
'1.0',
'/politica-de-datos',
'SURCO trata sus datos personales según la Ley 1581 de 2012 de Colombia y sus decretos reglamentarios.

Qué datos recogemos: su nombre, su número de celular, su correo electrónico si decide darlo, su municipio y la ubicación de los lotes que registre.

Para qué los usamos: únicamente para darle recomendaciones agroclimáticas de su finca y avisarle de pronósticos de lluvia y sequía. No vendemos sus datos ni los compartimos con terceros para publicidad.

Cómo los protegemos: su contraseña se guarda cifrada y nadie del equipo puede verla. El acceso a sus datos exige iniciar sesión.

Sus derechos: puede conocer, actualizar, corregir o pedir que borremos sus datos en cualquier momento, y revocar esta autorización.',
true);
