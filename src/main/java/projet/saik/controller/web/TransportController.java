package projet.saik.controller.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import projet.saik.service.TicketService;
import projet.saik.web.TicketForm;
import projet.saik.web.TravelDataDtos.TicketResponse;
import projet.saik.web.TripView;

@Controller
// Transports & billets du voyage sélectionné (relation 1-N Trip -> TransportTicket) :
// liste, création, modification et suppression via un formulaire Thymeleaf.
public class TransportController {

    private final TicketService ticketService;
    private final WebActions actions;

    public TransportController(TicketService ticketService, WebActions actions) {
        this.ticketService = ticketService;
        this.actions = actions;
    }

    // Affiche la page ; « edit » ouvre le formulaire pré-rempli avec le billet à modifier.
    @GetMapping("/transports")
    public String transports(Model model, @RequestParam(required = false) Long edit) {
        List<TicketResponse> tickets = loadTickets(model);
        TicketForm form = new TicketForm();
        Long editId = null;
        if (edit != null) {
            for (TicketResponse ticket : tickets) {
                if (ticket.id().equals(edit)) {
                    form = TicketForm.from(ticket);
                    editId = edit;
                }
            }
        }
        return page(model, tickets, form, editId, editId != null);
    }

    // Crée un billet sur le voyage sélectionné.
    @PostMapping("/transports/tickets")
    public String createTicket(@ModelAttribute("ticketForm") TicketForm form, BindingResult binding,
            Model model, RedirectAttributes flash) {
        TripView trip = (TripView) model.getAttribute("selectedTrip");
        if (trip == null) {
            flash.addFlashAttribute("error", "Créez ou sélectionnez d’abord un voyage.");
            return "redirect:/transports";
        }
        return save(form, binding, null, model, flash, "Titre enregistré.",
                () -> ticketService.createTicket(trip.getId(), actions.validate(form.toRequest())));
    }

    // Modifie un billet existant (le service vérifie qu’il appartient à la personne connectée).
    @PostMapping("/transports/tickets/{ticketId}")
    public String updateTicket(@PathVariable Long ticketId, @ModelAttribute("ticketForm") TicketForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        return save(form, binding, ticketId, model, flash, "Titre modifié.",
                () -> ticketService.updateTicket(ticketId, actions.validate(form.toRequest())));
    }

    // Supprime un titre (et la dépense automatique qu'il avait créée dans le budget).
    @PostMapping("/transports/tickets/{ticketId}/delete")
    public String deleteTicket(@PathVariable Long ticketId, RedirectAttributes flash) {
        actions.run(flash, "Titre supprimé.", () -> ticketService.deleteTicket(ticketId));
        return "redirect:/transports";
    }

    // Enregistrement commun à la création et à la modification.
    // En cas d’erreur, la page est réaffichée avec le formulaire ouvert et les valeurs saisies (pas de redirection).
    private String save(TicketForm form, BindingResult binding, Long editId, Model model,
            RedirectAttributes flash, String success, Runnable action) {
        try {
            if (binding.hasErrors()) {
                throw new IllegalArgumentException("Vérifiez les dates, horaires et montants saisis.");
            }
            action.run();
            flash.addFlashAttribute("message", success);
            return "redirect:/transports";
        } catch (ResponseStatusException | IllegalArgumentException ex) {
            model.addAttribute("error", actions.errorMessage(ex));
            return page(model, loadTickets(model), form, editId, true);
        }
    }

    // Prépare le modèle de la vue « transports ».
    private String page(Model model, List<TicketResponse> tickets, TicketForm form, Long editId, boolean open) {
        model.addAttribute("tickets", tickets);
        model.addAttribute("ticketTotal", tickets.stream().mapToDouble(t -> t.price() == null ? 0 : t.price()).sum());
        model.addAttribute("ticketForm", form);
        model.addAttribute("choices", TicketForm.CHOICES);
        model.addAttribute("editTicketId", editId);
        model.addAttribute("openTicketForm", open);
        return "transports";
    }

    // Billets du voyage sélectionné (fourni par LayoutAdvice), ou liste vide.
    private List<TicketResponse> loadTickets(Model model) {
        TripView selected = (TripView) model.getAttribute("selectedTrip");
        return selected == null ? List.of() : ticketService.getTickets(selected.getId());
    }
}
