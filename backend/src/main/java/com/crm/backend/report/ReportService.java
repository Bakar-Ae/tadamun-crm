package com.crm.backend.report;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;

    public ReportService(
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository
    ) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummaryReport() {
        return new ReportSummaryResponse(
                customerRepository.count(),
                customerRepository.countByStatus(CustomerStatus.ACTIVE),
                customerRepository.countByStatus(CustomerStatus.ARCHIVED),
                leadRepository.count(),
                leadRepository.countByStatus(LeadStatus.NEW),
                leadRepository.countByStatus(LeadStatus.QUALIFIED),
                leadRepository.countByStatus(LeadStatus.CONVERTED),
                leadRepository.countByStatus(LeadStatus.LOST),
                taskRepository.count(),
                taskRepository.countByStatus(TaskStatus.OPEN),
                taskRepository.countByStatus(TaskStatus.IN_PROGRESS),
                taskRepository.countByStatus(TaskStatus.COMPLETED),
                taskRepository.countByStatus(TaskStatus.CANCELLED)
        );
    }
}