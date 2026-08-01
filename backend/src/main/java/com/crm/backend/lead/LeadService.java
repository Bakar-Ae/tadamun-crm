package com.crm.backend.lead;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.lead.dto.CreateLeadRequest;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.lead.dto.UpdateLeadRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
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
    private final DataScopeService dataScopeService;
    private final CurrentOrganizationProvider currentOrganizationProvider;

    public LeadService(
            LeadRepository leadRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadMapper leadMapper,
            AuditLogService auditLogService,
            DataScopeService dataScopeService,
            CurrentOrganizationProvider currentOrganizationProvider
    ) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.leadMapper = leadMapper;
        this.auditLogService = auditLogService;
        this.dataScopeService = dataScopeService;
        this.currentOrganizationProvider = currentOrganizationProvider;
    }

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        User assignee = resolveAssignee(request.assignedToUserId(), context);

        Lead lead = new Lead();
        lead.setOrganization(
                currentOrganizationProvider.getOrganizationReference()
        );
        lead.setFullName(request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompanyName(request.companyName());
        lead.setSource(request.source());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setAssignedToUser(assignee);
        lead.setStatus(LeadStatus.NEW);

        Lead savedLead = leadRepository.save(lead);

        auditLogService.log(
                context.userId(),
                "LEAD_CREATED",
                "LEAD",
                savedLead.getId(),
                "{\"name\":\"" + savedLead.getFullName() + "\"}"
        );

        log.info(
                "Lead created. leadId={}, assignedToUserId={}",
                savedLead.getId(),
                assignee.getId()
        );

        return leadMapper.toResponse(savedLead);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeads(
            String keyword,
            LeadStatus status,
            Pageable pageable
    ) {
        DataScopeContext context = dataScopeService.currentContext();

        return leadRepository.searchAccessibleLeadsInOrganization(
                currentOrganizationProvider.getOrganizationId(),
                keyword,
                status,
                context.scope() == DataScope.ALL,
                context.scope() == DataScope.TEAM,
                context.userId(),
                context.teamId(),
                pageable
        ).map(leadMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        return leadMapper.toResponse(findAccessibleLeadOrThrow(id, context));
    }

    @Transactional
    public LeadResponse updateLead(Long id, UpdateLeadRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Lead lead = findAccessibleLeadOrThrow(id, context);

        lead.setFullName(request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompanyName(request.companyName());
        lead.setSource(request.source());
        lead.setEstimatedValue(request.estimatedValue());

        if (request.assignedToUserId() != null || lead.getAssignedToUser() == null) {
            lead.setAssignedToUser(
                    resolveAssignee(request.assignedToUserId(), context)
            );
        }

        lead.setStatus(request.status());

        auditLogService.log(
                context.userId(),
                "LEAD_UPDATED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName()
                        + "\",\"status\":\"" + lead.getStatus() + "\"}"
        );

        log.info("Lead updated. leadId={}, status={}", lead.getId(), lead.getStatus());
        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse archiveLead(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        Lead lead = findAccessibleLeadOrThrow(id, context);
        lead.setStatus(LeadStatus.ARCHIVED);

        auditLogService.log(
                context.userId(),
                "LEAD_ARCHIVED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName() + "\"}"
        );

        log.info("Lead archived. leadId={}", lead.getId());
        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse convertLead(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        Lead lead = findAccessibleLeadOrThrow(id, context);

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new IllegalArgumentException("Lead already converted");
        }

        if (lead.getStatus() == LeadStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived leads cannot be converted");
        }

        if (hasText(lead.getEmail()) && customerRepository.existsByOrganizationIdAndEmail(
                currentOrganizationProvider.getOrganizationId(),
                lead.getEmail()
        )) {
            throw new IllegalArgumentException("Customer email already exists");
        }

        Long assignedUserId = lead.getAssignedToUser() == null
                ? null
                : lead.getAssignedToUser().getId();
        User customerOwner = resolveAssignee(assignedUserId, context);

        Customer customer = new Customer();
        customer.setName(hasText(lead.getCompanyName())
                ? lead.getCompanyName()
                : lead.getFullName());
        customer.setEmail(lead.getEmail());
        customer.setPhone(lead.getPhone());
        customer.setCompanyName(lead.getCompanyName());
        customer.setCustomerType(hasText(lead.getCompanyName())
                ? CustomerType.COMPANY
                : CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setOwnerUser(customerOwner);
        customer.setOrganization(
                currentOrganizationProvider.getOrganizationReference()
        );

        Customer savedCustomer = customerRepository.save(customer);

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedCustomer(savedCustomer);

        auditLogService.log(
                context.userId(),
                "LEAD_CONVERTED",
                "LEAD",
                lead.getId(),
                "{\"name\":\"" + lead.getFullName()
                        + "\",\"customerId\":" + savedCustomer.getId() + "}"
        );

        log.info(
                "Lead converted. leadId={}, customerId={}",
                lead.getId(),
                savedCustomer.getId()
        );

        return leadMapper.toResponse(lead);
    }

    private Lead findAccessibleLeadOrThrow(
            Long id,
            DataScopeContext context
    ) {
        return leadRepository.findAccessibleByIdInOrganization(
                        id,
                        currentOrganizationProvider.getOrganizationId(),
                        context.scope() == DataScope.ALL,
                        context.scope() == DataScope.TEAM,
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lead not found")
                );
    }

    private User resolveAssignee(
            Long requestedUserId,
            DataScopeContext context
    ) {
        Long assigneeId = requestedUserId == null
                ? context.userId()
                : requestedUserId;

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));

        currentOrganizationProvider.requireActiveUserMembership(
                assignee.getId()
        );

        if (assignee.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Assigned user must be active");
        }

        Long assigneeTeamId = assignee.getTeam() == null
                ? null
                : assignee.getTeam().getId();

        dataScopeService.requireAccess(
                context,
                assignee.getId(),
                assigneeTeamId
        );

        return assignee;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
