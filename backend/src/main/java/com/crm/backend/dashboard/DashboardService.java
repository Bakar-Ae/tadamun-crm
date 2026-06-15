package com.crm.backend.dashboard;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import com.crm.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;

    public DashboardService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                userRepository.count(),
                customerRepository.countByStatus(CustomerStatus.ACTIVE),
                customerRepository.countByStatus(CustomerStatus.ARCHIVED),
                leadRepository.countByStatus(LeadStatus.NEW)
                        + leadRepository.countByStatus(LeadStatus.CONTACTED)
                        + leadRepository.countByStatus(LeadStatus.QUALIFIED),
                taskRepository.countByStatus(TaskStatus.OPEN),
                taskRepository.countByStatus(TaskStatus.COMPLETED)
        );
    }
}