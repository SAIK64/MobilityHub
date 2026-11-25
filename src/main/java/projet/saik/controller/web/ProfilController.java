package projet.saik.controller.web;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import projet.saik.service.ProfileService;
import projet.saik.web.TravelDataDtos.ProfileRequest;
import projet.saik.web.TravelDataDtos.ProfileResponse;

@Controller
// Profil de la personne connectée : affichage du formulaire pré-rempli et enregistrement.
public class ProfilController {

    private final ProfileService profileService;
    private final WebActions actions;

    public ProfilController(ProfileService profileService, WebActions actions) {
        this.profileService = profileService;
        this.actions = actions;
    }

    @GetMapping("/profil")
    public String profil(Model model) {
        ProfileResponse profile;
        try {
            profile = profileService.getProfile();
        } catch (ResponseStatusException ex) {
            profile = new ProfileResponse("", "", "", "", "", "", "");
        }
        model.addAttribute("profile", profile);
        model.addAttribute("age", age(profile.birthDate()));
        return "profil";
    }

    @PostMapping("/profil")
    public String update(@RequestParam String firstName, @RequestParam String lastName,
            @RequestParam(required = false) String birthDate, @RequestParam(required = false) String nationality,
            @RequestParam String email, @RequestParam(required = false) String phone,
            @RequestParam(required = false) String passport, @RequestParam(required = false) String currentPassword,
            RedirectAttributes flash) {
        actions.run(flash, "Profil enregistré.", () -> profileService.updateProfile(actions.validate(new ProfileRequest(
                firstName, lastName, trim(birthDate), trim(nationality), email, trim(phone), trim(passport),
                currentPassword == null || currentPassword.isEmpty() ? null : currentPassword))));
        return "redirect:/profil";
    }

    // Âge calculé à partir d'une date AAAA-MM-JJ, ou null si elle est absente / invalide.
    private static Integer age(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) return null;
        try {
            int years = Period.between(LocalDate.parse(birthDate.trim()), LocalDate.now()).getYears();
            return years >= 0 ? years : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
