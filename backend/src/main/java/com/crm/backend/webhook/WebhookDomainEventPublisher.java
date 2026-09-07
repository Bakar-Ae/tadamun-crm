package com.crm.backend.webhook;

import com.crm.backend.contact.Contact;
import com.crm.backend.customer.Customer;
import com.crm.backend.lead.Lead;
import com.crm.backend.note.Note;
import com.crm.backend.task.CrmTask;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookDomainEventPublisher {

    private final WebhookEventPublisher eventPublisher;

    WebhookDomainEventPublisher(WebhookEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void customerCreated(Customer customer) {
        publish(
                WebhookEventType.CUSTOMER_CREATED,
                "CUSTOMER",
                customer.getId(),
                "customer",
                customerSnapshot(customer)
        );
    }

    public void customerUpdated(
            Customer customer,
            List<String> changedFields
    ) {
        Map<String, Object> data = data(
                "customer",
                customerSnapshot(customer)
        );
        data.put("changedFields", List.copyOf(changedFields));
        eventPublisher.publish(
                WebhookEventType.CUSTOMER_UPDATED,
                "CUSTOMER",
                customer.getId(),
                data
        );
    }

    public void customerArchived(Customer customer) {
        publishStatus(
                WebhookEventType.CUSTOMER_ARCHIVED,
                "CUSTOMER",
                customer.getId(),
                "customer",
                customer.getStatus().name()
        );
    }

    public void customerRestored(Customer customer) {
        publishStatus(
                WebhookEventType.CUSTOMER_RESTORED,
                "CUSTOMER",
                customer.getId(),
                "customer",
                customer.getStatus().name()
        );
    }

    public void leadCreated(Lead lead) {
        publish(
                WebhookEventType.LEAD_CREATED,
                "LEAD",
                lead.getId(),
                "lead",
                leadSnapshot(lead)
        );
    }

    public void leadUpdated(Lead lead, List<String> changedFields) {
        Map<String, Object> data = data("lead", leadSnapshot(lead));
        data.put("changedFields", List.copyOf(changedFields));
        eventPublisher.publish(
                WebhookEventType.LEAD_UPDATED,
                "LEAD",
                lead.getId(),
                data
        );
    }

    public void leadArchived(Lead lead) {
        publishStatus(
                WebhookEventType.LEAD_ARCHIVED,
                "LEAD",
                lead.getId(),
                "lead",
                lead.getStatus().name()
        );
    }

    public void leadConverted(Lead lead, Customer customer) {
        eventPublisher.publish(
                WebhookEventType.LEAD_CONVERTED,
                "LEAD",
                lead.getId(),
                Map.of(
                        "leadId", lead.getId(),
                        "customerId", customer.getId()
                )
        );
    }

    public void contactCreated(Contact contact) {
        publish(
                WebhookEventType.CONTACT_CREATED,
                "CONTACT",
                contact.getId(),
                "contact",
                contactSnapshot(contact)
        );
    }

    public void contactUpdated(
            Contact contact,
            List<String> changedFields
    ) {
        Map<String, Object> data = data(
                "contact",
                contactSnapshot(contact)
        );
        data.put("changedFields", List.copyOf(changedFields));
        eventPublisher.publish(
                WebhookEventType.CONTACT_UPDATED,
                "CONTACT",
                contact.getId(),
                data
        );
    }

    public void taskCreated(CrmTask task) {
        publish(
                WebhookEventType.TASK_CREATED,
                "TASK",
                task.getId(),
                "task",
                taskSnapshot(task)
        );
    }

    public void taskUpdated(CrmTask task, List<String> changedFields) {
        Map<String, Object> data = data("task", taskSnapshot(task));
        data.put("changedFields", List.copyOf(changedFields));
        eventPublisher.publish(
                WebhookEventType.TASK_UPDATED,
                "TASK",
                task.getId(),
                data
        );
    }

    public void taskCompleted(CrmTask task) {
        publish(
                WebhookEventType.TASK_COMPLETED,
                "TASK",
                task.getId(),
                "task",
                nullableMap(
                        "id", task.getId(),
                        "status", task.getStatus().name(),
                        "assignedToUserId", id(task.getAssignedToUser())
                )
        );
    }

    public void noteCreated(Note note) {
        eventPublisher.publish(
                WebhookEventType.NOTE_CREATED,
                "NOTE",
                note.getId(),
                data(
                        "note",
                        nullableMap(
                                "id", note.getId(),
                                "customerId", id(note.getCustomer()),
                                "leadId", id(note.getLead()),
                                "createdByUserId", id(
                                        note.getCreatedByUser()
                                )
                        )
                )
        );
    }

    private Map<String, Object> customerSnapshot(Customer customer) {
        return nullableMap(
                "id", customer.getId(),
                "name", customer.getName(),
                "companyName", customer.getCompanyName(),
                "customerType", customer.getCustomerType().name(),
                "status", customer.getStatus().name(),
                "ownerUserId", id(customer.getOwnerUser())
        );
    }

    private Map<String, Object> leadSnapshot(Lead lead) {
        return nullableMap(
                "id", lead.getId(),
                "fullName", lead.getFullName(),
                "companyName", lead.getCompanyName(),
                "source", lead.getSource(),
                "estimatedValue", lead.getEstimatedValue(),
                "status", lead.getStatus().name(),
                "assignedToUserId", id(lead.getAssignedToUser())
        );
    }

    private Map<String, Object> contactSnapshot(Contact contact) {
        return nullableMap(
                "id", contact.getId(),
                "customerId", id(contact.getCustomer()),
                "fullName", contact.getFullName(),
                "position", contact.getPosition(),
                "status", contact.getStatus().name()
        );
    }

    private Map<String, Object> taskSnapshot(CrmTask task) {
        return nullableMap(
                "id", task.getId(),
                "title", task.getTitle(),
                "status", task.getStatus().name(),
                "priority", task.getPriority().name(),
                "dueDate", task.getDueDate() == null
                        ? null
                        : task.getDueDate().toString(),
                "assignedToUserId", id(task.getAssignedToUser()),
                "customerId", id(task.getCustomer()),
                "leadId", id(task.getLead())
        );
    }

    private void publishStatus(
            WebhookEventType eventType,
            String aggregateType,
            Long aggregateId,
            String dataKey,
            String status
    ) {
        publish(
                eventType,
                aggregateType,
                aggregateId,
                dataKey,
                Map.of("id", aggregateId, "status", status)
        );
    }

    private void publish(
            WebhookEventType eventType,
            String aggregateType,
            Long aggregateId,
            String dataKey,
            Map<String, ?> value
    ) {
        eventPublisher.publish(
                eventType,
                aggregateType,
                aggregateId,
                data(dataKey, value)
        );
    }

    private Map<String, Object> data(String key, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return data;
    }

    private Map<String, Object> nullableMap(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index].toString(), values[index + 1]);
        }
        return result;
    }

    private Long id(com.crm.backend.user.User user) {
        return user == null ? null : user.getId();
    }

    private Long id(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private Long id(Lead lead) {
        return lead == null ? null : lead.getId();
    }
}
