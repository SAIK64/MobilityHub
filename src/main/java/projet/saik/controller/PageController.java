package projet.saik.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

	@GetMapping({ "/", "/dashboard" })
	public String dashboard() {
		return "dashboard";
	}

	@GetMapping("/transports")
	public String transports() {
		return "transports";
	}

	@GetMapping("/budget")
	public String budget() {
		return "budget";
	}

	@GetMapping("/securite")
	public String securite() {
		return "securite";
	}

	@GetMapping("/profil")
	public String profil() {
		return "profil";
	}
}
