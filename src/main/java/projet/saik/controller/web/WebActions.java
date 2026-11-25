package projet.saik.controller.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

// Outils partagés par les contrôleurs de pages : validation des formulaires
// et transformation des erreurs du service en message affiché après redirection.
@Component
public class WebActions {

    private final Validator validator;

    public WebActions(Validator validator) {
        this.validator = validator;
    }

    // Exécute une action du service ; affiche « success » si tout va bien, sinon le message d'erreur.
    public boolean run(RedirectAttributes flash, String success, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("message", success);
            return true;
        } catch (ResponseStatusException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", errorMessage(ex));
            return false;
        }
    }

    // Message lisible pour une erreur métier (service) ou de validation (formulaire).
    public String errorMessage(RuntimeException ex) {
        if (ex instanceof ResponseStatusException status) {
            return status.getReason() != null ? status.getReason() : "Action impossible.";
        }
        return ex.getMessage();
    }

    // Applique les contraintes (@NotBlank, @Pattern…) déclarées sur les DTO, comme le fait @Valid pour l'API.
    public <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(" · ")));
        }
        return request;
    }
}
