package projet.saik.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import projet.saik.service.AccountService;
import projet.saik.web.AuthRequest;

@Controller
// Gère les pages HTML de connexion et d’inscription ainsi que leurs formulaires.
public class AuthController {
    // Délègue la création et la validation des comptes au service métier.
    private final AccountService accounts;

    // Spring injecte le service nécessaire au traitement des inscriptions.
    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    // Affiche la page de connexion Thymeleaf.
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    // Prépare un formulaire vide avant d’afficher la page d’inscription.
    public String register(Model model) {
        model.addAttribute("form", new AuthRequest());
        return "register";
    }

    @PostMapping("/register")
    // Valide les champs puis crée le compte envoyé par le formulaire.
    public String register(@Valid @ModelAttribute("form") AuthRequest form, BindingResult result) {
        // Réaffiche le formulaire si la validation des champs a échoué.
        if (result.hasErrors()) {
            form.setPassword("");
            return "register";
        }
        try {
            // Crée le compte après validation des champs.
            accounts.register(form.getEmail(), form.getPassword());
        } catch (ResponseStatusException ex) {
            // Associe les erreurs métier aux champs correspondants du formulaire.
            if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                result.rejectValue("email", "email.exists", ex.getReason());
            } else if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                result.rejectValue("password", "password.invalid", ex.getReason());
            } else {
                // Laisse remonter les erreurs qui ne correspondent pas aux cas affichables.
                throw ex;
            }
            form.setPassword("");
            return "register";
        }
        // Redirige vers la connexion après une inscription réussie.
        return "redirect:/login?registered";
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    // Rend le jeton CSRF disponible au JavaScript pour ses requêtes de modification.
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
