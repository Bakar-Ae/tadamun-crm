package com.crm.backend.publicapi;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.customer.CustomerMapper;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.customer.dto.CustomerResponse;
import com.crm.backend.lead.LeadMapper;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.publicapi.key.PublicApiScope;
import com.crm.backend.publicapi.security.PublicApiContextHolder;
import com.crm.backend.publicapi.security.PublicApiScopeGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PublicApiReadService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Set<String> CUSTOMER_SORT_FIELDS = Set.of(
            "id",
            "name",
            "companyName",
            "status",
            "createdAt",
            "updatedAt"
    );
    private static final Set<String> LEAD_SORT_FIELDS = Set.of(
            "id",
            "fullName",
            "companyName",
            "status",
            "estimatedValue",
            "createdAt",
            "updatedAt"
    );

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;
    private final PublicApiScopeGuard scopeGuard;

    public PublicApiReadService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            LeadRepository leadRepository,
            LeadMapper leadMapper,
            PublicApiScopeGuard scopeGuard
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.leadRepository = leadRepository;
        this.leadMapper = leadMapper;
        this.scopeGuard = scopeGuard;
    }

    public Page<CustomerResponse> getCustomers(
            String keyword,
            CustomerStatus status,
            CustomerType customerType,
            Pageable pageable
    ) {
        scopeGuard.require(PublicApiScope.CUSTOMERS_READ);
        validatePageable(pageable, CUSTOMER_SORT_FIELDS);

        return customerRepository.searchAccessibleCustomersInOrganization(
                organizationId(),
                cleanKeyword(keyword),
                status,
                customerType,
                true,
                false,
                null,
                null,
                pageable
        ).map(customerMapper::toResponse);
    }

    public CustomerResponse getCustomer(Long id) {
        scopeGuard.require(PublicApiScope.CUSTOMERS_READ);
        return customerRepository.findAccessibleByIdInOrganization(
                id,
                organizationId(),
                true,
                false,
                null,
                null
        ).map(customerMapper::toResponse).orElseThrow(() ->
                new ResourceNotFoundException("Customer not found")
        );
    }

    public Page<LeadResponse> getLeads(
            String keyword,
            LeadStatus status,
            Pageable pageable
    ) {
        scopeGuard.require(PublicApiScope.LEADS_READ);
        validatePageable(pageable, LEAD_SORT_FIELDS);

        return leadRepository.searchAccessibleLeadsInOrganization(
                organizationId(),
                cleanKeyword(keyword),
                status,
                true,
                false,
                null,
                null,
                pageable
        ).map(leadMapper::toResponse);
    }

    public LeadResponse getLead(Long id) {
        scopeGuard.require(PublicApiScope.LEADS_READ);
        return leadRepository.findAccessibleByIdInOrganization(
                id,
                organizationId(),
                true,
                false,
                null,
                null
        ).map(leadMapper::toResponse).orElseThrow(() ->
                new ResourceNotFoundException("Lead not found")
        );
    }

    private Long organizationId() {
        return PublicApiContextHolder.getRequired().organizationId();
    }

    private String cleanKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String cleaned = keyword.trim();
        if (cleaned.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Keyword must contain at most 100 characters"
            );
        }
        return cleaned;
    }

    private void validatePageable(
            Pageable pageable,
            Set<String> allowedSortFields
    ) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Public API page size must not exceed 100"
            );
        }

        pageable.getSort().forEach(order -> {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Unsupported public API sort field: "
                                + order.getProperty()
                );
            }
        });
    }
}
