package projet.saik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Point d’entrée Spring Boot : lance l’application et active l’auto-configuration.
public class SaikApplication {

	// Démarre le contexte Spring et le serveur web embarqué.
	public static void main(String[] args) {
		SpringApplication.run(SaikApplication.class, args);
	}

}
