package com.crm.backend.webhook;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.note.Note;
import com.crm.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebhookDomainEventPublisherTest {

    private WebhookEventPublisher eventPublisher;
    private WebhookDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(WebhookEventPublisher.class);
        publisher = new WebhookDomainEventPublisher(eventPublisher);
    }

    @Test
    @SuppressWarnings("unchecked")
    void customerPayloadShouldExcludeEmailAndPhone() {
        User owner = new User();
        owner.setId(7L);
        Customer customer = new Customer();
        customer.setId(9L);
        customer.setName("Acme");
        customer.setEmail("private@example.com");
        customer.setPhone("+252000000");
        customer.setCompanyName("Acme Ltd");
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setOwnerUser(owner);

        publisher.customerCreated(customer);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(
                Map.class
        );
        verify(eventPublisher).publish(
                eq(WebhookEventType.CUSTOMER_CREATED),
                eq("CUSTOMER"),
                eq(9L),
                captor.capture()
        );
        Map<String, Object> snapshot = (Map<String, Object>) captor
                .getValue().get("customer");
        assertEquals("Acme", snapshot.get("name"));
        assertFalse(snapshot.containsKey("email"));
        assertFalse(snapshot.containsKey("phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notePayloadShouldNeverExposeNoteBody() {
        User creator = new User();
        creator.setId(7L);
        Note note = new Note();
        note.setId(11L);
        note.setContent("Highly confidential note body");
        note.setCreatedByUser(creator);

        publisher.noteCreated(note);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(
                Map.class
        );
        verify(eventPublisher).publish(
                eq(WebhookEventType.NOTE_CREATED),
                eq("NOTE"),
                eq(11L),
                captor.capture()
        );
        Map<String, Object> snapshot = (Map<String, Object>) captor
                .getValue().get("note");
        assertEquals(7L, snapshot.get("createdByUserId"));
        assertFalse(snapshot.containsKey("content"));
        assertFalse(captor.getValue().toString().contains("confidential"));
    }
}
