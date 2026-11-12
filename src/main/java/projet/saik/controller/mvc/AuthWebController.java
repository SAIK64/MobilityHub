package projet.saik.controller.mvc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import projet.saik.dto.RegisterRequest;
import projet.saik.service.EmailDejaUtiliseException;
import projet.saik.service.UtilisateurService;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final UtilisateurService utilisateurService;

    @GetMapping("/")
    public String accueil(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("roles", authentication.getAuthorities());
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String formulaireInscription(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String inscription(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            utilisateurService.inscrire(form.getEmail(), form.getPassword());
        } catch (EmailDejaUtiliseException e) {
            result.rejectValue("email", "email.exists", "Cet email est déjà utilisé");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
