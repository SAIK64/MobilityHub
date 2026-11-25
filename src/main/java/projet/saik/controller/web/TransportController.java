package projet.saik.controller.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import projet.saik.service.TicketService;
import projet.saik.web.TravelDataDtos.TicketResponse;
import projet.saik.web.TripView;

@Controller
// Transports & billets : affiche les titres du voyage sélectionné (relation 1-N Trip -> TransportTicket).
public class TransportController {

    private final TicketService ticketService;
    private final WebActions actions;

    public TransportController(TicketService ticketService, WebActions actions) {
        this.ticketService = ticketService;
        this.actions = actions;
    }

    @GetMapping("/transports")
    public String transports(Model model) {
        TripView selected = (TripView) model.getAttribute("selectedTrip");
        List<TicketResponse> tickets = selected == null ? List.of() : ticketService.getTickets(selected.getId());
        model.addAttribute("tickets", tickets);
        model.addAttribute("ticketTotal", tickets.stream().mapToDouble(t -> t.price() == null ? 0 : t.price()).sum());
        return "transports";
    }

    // Supprime un titre (et la dépense automatique qu'il avait créée dans le budget).
    @PostMapping("/transports/tickets/{ticketId}/delete")
    public String deleteTicket(@PathVariable Long ticketId, RedirectAttributes flash) {
        actions.run(flash, "Titre supprimé.", () -> ticketService.deleteTicket(ticketId));
        return "redirect:/transports";
    }
}
