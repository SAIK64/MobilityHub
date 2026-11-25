package projet.saik.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Pages sans données propres : accueil public et kit de sécurité.
// Le voyage sélectionné (en-tête, kit de sécurité) est ajouté au modèle par LayoutAdvice.
// La racine « / » est gérée par DashboardController.
public class PageController {

	@GetMapping("/accueil")
	public String accueil() {
		return "accueil";
	}

	@GetMapping("/securite")
	// Affiche le guide de sécurité et d’urgence pour le voyage sélectionné.
	public String securite() {
		return "securite";
	}
}
