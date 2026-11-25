package projet.saik;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // Isole le contexte de test de la base persistante utilisée par l’application.
		"spring.datasource.url=jdbc:h2:mem:saik-test;DB_CLOSE_DELAY=-1",
        // Supprime le schéma mémoire après les tests.
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
// Test de fumée qui vérifie que l’application démarre avec une base de test isolée.
class SaikApplicationTests {

	@Test
	// Vérifie que le contexte Spring démarre correctement.
	void contextLoads() {
	}

}
