package com.crm.backend.note;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.dto.CreateNoteRequest;
import com.crm.backend.note.dto.NoteResponse;
import com.crm.backend.note.dto.UpdateNoteRequest;
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

    public NoteService(
            NoteRepository noteRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            NoteMapper noteMapper
    ) {
        this.noteRepository = noteRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.noteMapper = noteMapper;
    }

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request, Long currentUserId) {
        validateOwner(request.customerId(), request.leadId());

        Note note = new Note();
        note.setContent(request.content());
        note.setCustomer(findCustomerOrNull(request.customerId()));
        note.setLead(findLeadOrNull(request.leadId()));
        note.setCreatedByUser(findUserOrThrow(currentUserId));

        return noteMapper.toResponse(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getCustomerNotes(Long customerId, Pageable pageable) {
        return noteRepository.findByCustomerId(customerId, pageable)
                .map(noteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getLeadNotes(Long leadId, Pageable pageable) {
        return noteRepository.findByLeadId(leadId, pageable)
                .map(noteMapper::toResponse);
    }

    @Transactional
    public NoteResponse updateNote(Long id, UpdateNoteRequest request) {
        Note note = findNoteOrThrow(id);
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

    private Note findNoteOrThrow(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));
    }

    private Customer findCustomerOrNull(Long id) {
        if (id == null) {
            return null;
        }

        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private Lead findLeadOrNull(Long id) {
        if (id == null) {
            return null;
        }

        return leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}