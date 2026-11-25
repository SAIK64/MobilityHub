package projet.saik.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Associe les URL des pages principales aux vues Thymeleaf correspondantes.
public class PageController {

	@GetMapping("/")
	public String index(Principal principal) {
		return principal == null ? "accueil" : "dashboard";
	}

	@GetMapping("/accueil")
	public String accueil() {
		return "accueil";
	}

	@GetMapping("/dashboard")
	// Affiche le tableau de bord pour les utilisateurs connectés.
	public String dashboard() {
		return "dashboard";
	}

	@GetMapping("/transports")
	// Affiche la page des transports et billets.
	public String transports() {
		return "transports";
	}

	@GetMapping("/budget")
	// Affiche la page du budget et des dépenses.
	public String budget() {
		return "budget";
	}

	@GetMapping("/securite")
	// Affiche le guide de sécurité et d’urgence.
	public String securite() {
		return "securite";
	}

	@GetMapping("/profil")
	// Affiche la page de gestion du profil.
	public String profil() {
		return "profil";
	}
}
