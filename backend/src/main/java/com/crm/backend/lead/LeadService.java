package com.crm.backend.lead;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.lead.dto.CreateLeadRequest;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.lead.dto.UpdateLeadRequest;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {
    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;
    private final AuditLogService auditLogService;

    public LeadService(
            LeadRepository leadRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadMapper leadMapper,
            AuditLogService auditLogService
    ) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.leadMapper = leadMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request, Long actorUserId) {
        Lead lead = new Lead();
        lead.setFullName(request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompanyName(request.companyName());
        lead.setSource(request.source());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setAssignedToUser(findUserOrNull(request.assignedToUserId()));
        lead.setStatus(LeadStatus.NEW);

        Lead savedLead = leadRepository.save(lead);
        auditLogService.log(
                actorUserId,
                "LEAD_CREATED",
                "LEAD",
                savedLead.getId(),
                "{\"name\":\"" + savedLead.getFullName() + "\"}"
        );

        log.info("Lead created. leadId={}, assignedToUserId={}",
                savedLead.getId(), request.assignedToUserId());

        return leadMapper.toResponse(savedLead);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeads(String keyword, LeadStatus status, Pageable pageable) {
        return leadRepository.searchLeads(keyword, status, pageable)
                .map(leadMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        return leadMapper.toResponse(findLeadOrThrow(id));
    }

    @Transactional
    public LeadResponse updateLead(Long id, UpdateLeadRequest request, Long actorUserId) {
        Lead lead = findLeadOrThrow(id);

        lead.setFullName(request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompanyName(request.companyName());
        lead.setSource(request.source());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setAssignedToUser(findUserOrNull(request.assignedToUserId()));
        lead.setStatus(request.status());
        auditLogService.log(
                actorUserId,
                "LEAD_UPDATED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName() + "\",\"status\":\"" + lead.getStatus() + "\"}"
        );
        log.info("Lead updated. leadId={}, status={}", lead.getId(), lead.getStatus());

        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse archiveLead(Long id, Long actorUserId) {
        Lead lead = findLeadOrThrow(id);
        lead.setStatus(LeadStatus.ARCHIVED);

        auditLogService.log(
                actorUserId,
                "LEAD_ARCHIVED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName() + "\"}"
        );

        log.info("Lead archived. leadId={}", lead.getId());

        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse convertLead(Long id, Long actorUserId) {
        Lead lead = findLeadOrThrow(id);

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new IllegalArgumentException("Lead already converted");
        }

        if (lead.getStatus() == LeadStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived leads cannot be converted");
        }

        if (hasText(lead.getEmail()) && customerRepository.existsByEmail(lead.getEmail())) {
            throw new IllegalArgumentException("Customer email already exists");
        }

        Customer customer = new Customer();
        customer.setName(hasText(lead.getCompanyName()) ? lead.getCompanyName() : lead.getFullName());
        customer.setEmail(lead.getEmail());
        customer.setPhone(lead.getPhone());
        customer.setCompanyName(lead.getCompanyName());
        customer.setCustomerType(hasText(lead.getCompanyName()) ? CustomerType.COMPANY : CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer savedCustomer = customerRepository.save(customer);

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedCustomer(savedCustomer);

        auditLogService.log(
                actorUserId,
                "LEAD_CONVERTED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName() + "\",\"customerId\":" + savedCustomer.getId() + "}"
        );

        log.info("Lead converted. leadId={}, customerId={}", lead.getId(), savedCustomer.getId());

        return leadMapper.toResponse(lead);
    }

    private Lead findLeadOrThrow(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
    }

    private User findUserOrNull(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
