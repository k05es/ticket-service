package com.k05es.ticketservice.mapper;

import com.k05es.ticketservice.dto.response.MessageResponse;
import com.k05es.ticketservice.dto.response.TicketResponse;
import com.k05es.ticketservice.model.Message;
import com.k05es.ticketservice.model.Ticket;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Преобразует модели заявок и сообщений в REST-ответы. */
@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                ticket.getStatus(), ticket.getCustomer().getId(), ticket.getCustomer().getUsername(),
                ticket.getSpecialist() != null ? ticket.getSpecialist().getId() : null,
                ticket.getSpecialist() != null ? ticket.getSpecialist().getUsername() : null,
                ticket.getCreatedAt(), ticket.getUpdatedAt(),
                ticket.getMessages().stream().map(this::toMessageResponse).collect(Collectors.toList()));
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(message.getId(), message.getText(), message.getSender().getId(),
                message.getSender().getUsername(), message.getSentAt());
    }
}