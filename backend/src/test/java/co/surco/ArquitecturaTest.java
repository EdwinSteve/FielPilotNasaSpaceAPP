package co.surco;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reglas del monolito modular (docs/arquitectura.md). Si una falla, el mensaje dice qué clase rompió qué regla.
 */
@AnalyzeClasses(packages = "co.surco", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

	private static final String RAIZ = "co.surco.";

	@ArchTest
	static final ArchRule dominioEsJavaPuro = noClasses().that().resideInAPackage("co.surco..dominio..")
		.should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
			"jakarta.servlet..", "tools.jackson..", "com.fasterxml.jackson..", "org.hibernate..")
		.as("El dominio no depende de Spring, JPA, Servlet ni Jackson");

	@ArchTest
	static final ArchRule modulosSeHablanPorSuApiPublica = classes().that().resideInAPackage("co.surco..")
		.should(new ArchCondition<JavaClass>("usar otros módulos solo por su API pública (el paquete raíz del módulo)") {
			@Override
			public void check(JavaClass clase, ConditionEvents eventos) {
				String propio = modulo(clase);
				for (Dependency d : clase.getDirectDependenciesFromSelf()) {
					JavaClass destino = d.getTargetClass();
					String otro = modulo(destino);
					boolean esOtroModulo = otro != null && !otro.equals(propio) && !otro.equals("compartido");
					if (esOtroModulo && !destino.getPackageName().equals(RAIZ + otro)) {
						eventos.add(SimpleConditionEvent.violated(d, d.getDescription()
							+ " → use la API pública de co.surco." + otro));
					}
				}
			}
		});

	@ArchTest
	static final ArchRule compartidoNoDependeDeModulos = noClasses().that().resideInAPackage("co.surco.compartido..")
		.should().dependOnClassesThat(new com.tngtech.archunit.base.DescribedPredicate<JavaClass>("un módulo") {
			@Override
			public boolean test(JavaClass c) {
				String m = modulo(c);
				return m != null && !m.equals("compartido");
			}
		});

	@ArchTest
	static final ArchRule entidadesJpaEnInfraestructura = classes().that().areAnnotatedWith(Entity.class)
		.should().resideInAPackage("..infraestructura..");

	@ArchTest
	static final ArchRule controladoresEnApi = classes().that().areAnnotatedWith(RestController.class)
		.should().resideInAnyPackage("..api..", "co.surco.compartido.web..");

	@ArchTest
	static final ArchRule apiNoTocaInfraestructura = noClasses().that().resideInAPackage("..api..")
		.should().dependOnClassesThat().resideInAPackage("..infraestructura..")
		.as("Los controladores usan casos de uso (aplicacion), no repositorios ni adaptadores");

	/** "co.surco.identidad.aplicacion" → "identidad"; fuera de co.surco o en la raíz → null. */
	static String modulo(JavaClass clase) {
		String paquete = clase.getPackageName();
		if (!paquete.startsWith(RAIZ)) {
			return null;
		}
		String resto = paquete.substring(RAIZ.length());
		int punto = resto.indexOf('.');
		return punto < 0 ? resto : resto.substring(0, punto);
	}
}
