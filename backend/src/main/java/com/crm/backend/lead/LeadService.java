package com.crm.backend.lead;

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
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;

    public LeadService(LeadRepository leadRepository, UserRepository userRepository, LeadMapper leadMapper) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.leadMapper = leadMapper;
    }

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
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
    public LeadResponse updateLead(Long id, UpdateLeadRequest request) {
        Lead lead = findLeadOrThrow(id);

        lead.setFullName(request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompanyName(request.companyName());
        lead.setSource(request.source());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setAssignedToUser(findUserOrNull(request.assignedToUserId()));
        lead.setStatus(request.status());
        log.info("Lead updated. leadId={}, status={}", lead.getId(), lead.getStatus());

        return leadMapper.toResponse(lead);
    }

    @Transactional
    public LeadResponse archiveLead(Long id) {
        Lead lead = findLeadOrThrow(id);
        lead.setStatus(LeadStatus.ARCHIVED);
        log.info("Lead archived. leadId={}", lead.getId());
        log.info("Lead archived. leadId={}", lead.getId());
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
}