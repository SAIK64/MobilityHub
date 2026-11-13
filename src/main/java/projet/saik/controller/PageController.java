package projet.saik.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Associe les URL des pages principales aux vues Thymeleaf correspondantes.
public class PageController {

	@GetMapping({ "/", "/dashboard" })
	// Affiche le tableau de bord sur la racine ou sur /dashboard.
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
