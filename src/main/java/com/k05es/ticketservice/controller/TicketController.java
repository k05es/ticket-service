package com.k05es.ticketservice.controller;

import com.k05es.ticketservice.dto.request.CompleteTicketRequest;
import com.k05es.ticketservice.dto.request.CreateMessageRequest;
import com.k05es.ticketservice.dto.request.CreateTicketRequest;
import com.k05es.ticketservice.dto.request.UpdateTicketRequest;
import com.k05es.ticketservice.dto.response.TicketResponse;
import com.k05es.ticketservice.exception.ResourceNotFoundException;
import com.k05es.ticketservice.mapper.TicketMapper;
import com.k05es.ticketservice.model.CustomerUser;
import com.k05es.ticketservice.model.SpecialistUser;
import com.k05es.ticketservice.model.Ticket;
import com.k05es.ticketservice.model.TicketStatus;
import com.k05es.ticketservice.model.User;
import com.k05es.ticketservice.service.TicketService;
import com.k05es.ticketservice.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST-контроллер для CRUD-операций с заявками. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserService userService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, UserService userService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.ticketMapper = ticketMapper;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        User foundCustomer = userService.findUserById(request.getCustomerId());
        if (!(foundCustomer instanceof CustomerUser customer)) {
            throw new ResourceNotFoundException("Клиент не найден");
        }
        Ticket created = ticketService.createTicket(request.getTitle(), request.getDescription(), customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ticketMapper.toResponse(created));
    }

    @GetMapping
    public List<TicketResponse> getTickets(@RequestParam(required = false) TicketStatus status,
                                           @RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) Long specialistId,
                                           @RequestParam(required = false) String customerEmail) {
        return ticketService.searchTickets(status, customerId, specialistId, customerEmail).stream()
                .map(ticketMapper::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable Long id) {
        return ticketMapper.toResponse(ticketService.findTicketById(id));
    }

    @PutMapping("/{id}")
    public TicketResponse updateTicket(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketMapper.toResponse(ticketService.updateTicket(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        if (!ticketService.deleteTicket(id)) throw new ResourceNotFoundException("Заявка с ID " + id + " не найдена");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign/{specialistId}")
    public TicketResponse assignTicket(@PathVariable Long id, @PathVariable Long specialistId) {
        User foundSpecialist = userService.findUserById(specialistId);
        if (!(foundSpecialist instanceof SpecialistUser specialist)) {
            throw new ResourceNotFoundException("Специалист не найден");
        }
        ticketService.assignTicketToSpecialist(id, specialist);
        return ticketMapper.toResponse(ticketService.findTicketById(id));
    }

    @PostMapping("/{id}/complete")
    public TicketResponse completeTicket(@PathVariable Long id,
                                         @Valid @RequestBody CompleteTicketRequest request) {
        ticketService.completeTicket(id, request.getRating());
        return ticketMapper.toResponse(ticketService.findTicketById(id));
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancelTicket(@PathVariable Long id) {
        ticketService.cancelTicket(id);
        return ticketMapper.toResponse(ticketService.findTicketById(id));
    }

    @PostMapping("/{id}/messages")
    public TicketResponse addMessage(@PathVariable Long id, @Valid @RequestBody CreateMessageRequest request) {
        User sender = userService.findUserById(request.getSenderId());
        ticketService.addMessageToTicket(id, sender, request.getText());
        return ticketMapper.toResponse(ticketService.findTicketById(id));
    }
}