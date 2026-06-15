package com.crm.backend.task;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.task.dto.CreateTaskRequest;
import com.crm.backend.task.dto.TaskResponse;
import com.crm.backend.task.dto.UpdateTaskRequest;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskMapper taskMapper;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        CrmTask task = new CrmTask();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setStatus(TaskStatus.OPEN);
        task.setDueDate(request.dueDate());
        task.setAssignedToUser(findUserOrNull(request.assignedToUserId()));
        task.setCustomer(findCustomerOrNull(request.customerId()));
        task.setLead(findLeadOrNull(request.leadId()));

        CrmTask savedTask = taskRepository.save(task);

        log.info("Task created. taskId={}, assignedToUserId={}, status={}",
                savedTask.getId(), request.assignedToUserId(), savedTask.getStatus());

        return taskMapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(
            String keyword,
            TaskStatus status,
            TaskPriority priority,
            Long assignedToUserId,
            Long customerId,
            Long leadId,
            Pageable pageable
    ) {
        return taskRepository.searchTasks(keyword, status, priority, assignedToUserId, customerId, leadId, pageable)
                .map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return taskMapper.toResponse(findTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        CrmTask task = findTaskOrThrow(id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignedToUser(findUserOrNull(request.assignedToUserId()));
        task.setCustomer(findCustomerOrNull(request.customerId()));
        task.setLead(findLeadOrNull(request.leadId()));
        log.info("Task updated. taskId={}, assignedToUserId={}, status={}, priority={}",
                task.getId(), request.assignedToUserId(), task.getStatus(), task.getPriority());

        return taskMapper.toResponse(task);
    }

    private CrmTask findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    private User findUserOrNull(Long id) {
        if (id == null) {
            return null;
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));
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
}