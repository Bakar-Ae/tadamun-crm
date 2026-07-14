package com.crm.backend.note;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.dto.CreateNoteRequest;
import com.crm.backend.note.dto.NoteResponse;
import com.crm.backend.note.dto.UpdateNoteRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final NoteMapper noteMapper;
    private final DataScopeService dataScopeService;

    public NoteService(
            NoteRepository noteRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            NoteMapper noteMapper,
            DataScopeService dataScopeService
    ) {
        this.noteRepository = noteRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.noteMapper = noteMapper;
        this.dataScopeService = dataScopeService;
    }

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        validateOwner(request.customerId(), request.leadId());

        Note note = new Note();
        note.setContent(request.content());
        note.setCustomer(findAccessibleCustomerOrNull(request.customerId(), context));
        note.setLead(findAccessibleLeadOrNull(request.leadId(), context));
        note.setCreatedByUser(findUserOrThrow(context.userId()));

        return noteMapper.toResponse(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getCustomerNotes(Long customerId, Pageable pageable) {
        DataScopeContext context = dataScopeService.currentContext();
        findAccessibleCustomerOrNull(customerId, context);
        return noteRepository.findByCustomerId(customerId, pageable)
                .map(noteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getLeadNotes(Long leadId, Pageable pageable) {
        DataScopeContext context = dataScopeService.currentContext();
        findAccessibleLeadOrNull(leadId, context);
        return noteRepository.findByLeadId(leadId, pageable)
                .map(noteMapper::toResponse);
    }

    @Transactional
    public NoteResponse updateNote(Long id, UpdateNoteRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Note note = findAccessibleNoteOrThrow(id, context);
        note.setContent(request.content());
        return noteMapper.toResponse(note);
    }

    private void validateOwner(Long customerId, Long leadId) {
        boolean hasCustomer = customerId != null;
        boolean hasLead = leadId != null;

        if (hasCustomer == hasLead) {
            throw new IllegalArgumentException("Note must belong to either customer or lead");
        }
    }

    private Note findAccessibleNoteOrThrow(Long id, DataScopeContext context) {
        return noteRepository.findAccessibleById(
                        id,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));
    }

    private Customer findAccessibleCustomerOrNull(Long id, DataScopeContext context) {
        if (id == null) {
            return null;
        }

        return customerRepository.findAccessibleById(
                        id,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private Lead findAccessibleLeadOrNull(Long id, DataScopeContext context) {
        if (id == null) {
            return null;
        }

        return leadRepository.findAccessibleById(
                        id,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }
}
