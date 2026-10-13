package com.ngoconnect.service.impl;

import com.ngoconnect.dto.VolunteerEarningsResponse;
import com.ngoconnect.dto.VolunteerTaskRequest;
import com.ngoconnect.dto.VolunteerTaskResponse;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.NotificationService;
import com.ngoconnect.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final VolunteerTaskRepository taskRepository;
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final FoodDriveRepository foodDriveRepository;
    private final NotificationService notificationService;
    private final DonationClaimRepository donationClaimRepository;
    private final NgoRepository ngoRepository;

    @Override
    @Transactional
    public VolunteerTaskResponse assignTask(Long assignedByUserId, VolunteerTaskRequest request) {
        User assigner = userRepository.findById(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner user not found"));

        Volunteer volunteer = volunteerRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found with id: " + request.getVolunteerId()));

        if (!"ACTIVE".equals(volunteer.getAvailabilityStatus())) {
            throw new BadRequestException("Volunteer " + volunteer.getVolunteerCode() + " is currently INACTIVE and cannot be assigned tasks.");
        }

        // Validate paid task details
        boolean isPaid = Boolean.TRUE.equals(request.getIsPaid());
        BigDecimal paymentAmount = null;
        String currency = "INR";
        String paymentTerms = null;

        if (isPaid) {
            if (request.getPaymentAmount() == null) {
                throw new BadRequestException("Payment amount is required for paid tasks.");
            }
            if (request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Payment amount must be greater than zero for paid tasks.");
            }
            paymentAmount = request.getPaymentAmount();
            currency = request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency().trim() : "INR";
            paymentTerms = request.getPaymentTerms() != null ? request.getPaymentTerms().trim() : null;
        }

        Donation donation = null;
        Ngo assignerNgo = null;
        if (request.getDonationId() != null) {
            donation = donationRepository.findById(request.getDonationId())
                    .orElseThrow(() -> new BadRequestException("Donation not found with id: " + request.getDonationId()));

            DonationClaim claim = donationClaimRepository.findByDonationId(donation.getId())
                    .orElseThrow(() -> new BadRequestException("Donation has not been claimed yet. Cannot assign volunteer."));

            assignerNgo = ngoRepository.findByUserId(assignedByUserId).orElse(null);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(assigner.getRole().getName());
            boolean isDonor = donation.getDonorUser().getId().equals(assignedByUserId);

            if (assignerNgo != null) {
                if (!claim.getNgo().getId().equals(assignerNgo.getId())) {
                    throw new ForbiddenException("You cannot assign a volunteer to a donation claimed by another NGO.");
                }
            } else if (!isAdmin && !isDonor) {
                throw new ForbiddenException("You are not authorized to assign a volunteer for this donation.");
            }

            if ("COMPLETED".equalsIgnoreCase(donation.getStatus()) || "EXPIRED".equalsIgnoreCase(donation.getStatus())) {
                throw new BadRequestException("Cannot assign volunteer to a " + donation.getStatus() + " donation.");
            }
        }

        FoodDrive foodDrive = null;
        if (request.getFoodDriveId() != null) {
            foodDrive = foodDriveRepository.findById(request.getFoodDriveId()).orElse(null);
        }

        VolunteerTask task = VolunteerTask.builder()
                .volunteer(volunteer)
                .assignedByUser(assigner)
                .donation(donation)
                .foodDrive(foodDrive)
                .taskType(request.getTaskType().toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .pickupAddress(request.getPickupAddress())
                .destinationAddress(request.getDestinationAddress() != null ? request.getDestinationAddress() : request.getPickupAddress())
                .scheduledTime(request.getScheduledTime() != null ? request.getScheduledTime() : LocalDateTime.now().plusHours(2))
                .status("ASSIGNED")
                .isPaid(isPaid)
                .paymentAmount(paymentAmount)
                .currency(currency)
                .paymentTerms(paymentTerms)
                .paymentStatus("NOT_APPLICABLE") // Always NOT_APPLICABLE until verified completion
                .build();

        task = taskRepository.save(task);

        // Notify Volunteer
        String taskDetails = assigner.getName() + " assigned you a task: " + task.getTitle() + ". Pickup: " + task.getPickupAddress();
        if (isPaid) {
            taskDetails += " (Paid: " + currency + " " + paymentAmount + ")";
        }

        notificationService.sendNotification(
                volunteer.getUser().getId(),
                "New Task Assigned!",
                taskDetails,
                "VOLUNTEER_TASK_ASSIGNED",
                "VOLUNTEER_TASK",
                task.getId()
        );

        // Immediately notify original donor if task is linked to a donation
        if (donation != null) {
            String assignerDisplayName = assignerNgo != null ? assignerNgo.getNgoName() : assigner.getName();
            notificationService.sendNotification(
                    donation.getDonorUser().getId(),
                    "Volunteer Assigned",
                    assignerDisplayName + " has assigned volunteer " + volunteer.getUser().getName() + " to collect your donation \"" + donation.getTitle() + "\".",
                    "VOLUNTEER_ASSIGNED",
                    "DONATION",
                    donation.getId()
            );
        }

        return mapToResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerTaskResponse> getTasksForVolunteer(Long volunteerUserId) {
        Volunteer volunteer = volunteerRepository.findByUserId(volunteerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found for user id: " + volunteerUserId));

        return taskRepository.findByVolunteerId(volunteer.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerTaskResponse> getTasksAssignedByUser(Long userId) {
        return taskRepository.findByAssignedByUserId(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VolunteerTaskResponse updateTaskStatus(Long taskId, String newStatus, Long userId) {
        VolunteerTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAssignedVolunteer = task.getVolunteer() != null && task.getVolunteer().getUser().getId().equals(userId);
        boolean isAssigner = task.getAssignedByUser() != null && task.getAssignedByUser().getId().equals(userId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (!isAssignedVolunteer && !isAssigner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to update this volunteer task.");
        }

        String statusUpper = newStatus.toUpperCase();

        // Prevent modifying task status of an already paid task
        if ("PAID".equalsIgnoreCase(task.getPaymentStatus()) && !statusUpper.equalsIgnoreCase("COMPLETED")) {
            throw new BadRequestException("Cannot modify status of a completed task whose payment has already been confirmed as PAID.");
        }

        task.setStatus(statusUpper);

        if ("COMPLETED".equalsIgnoreCase(statusUpper)) {
            if (task.getDonation() != null && isAssignedVolunteer) {
                if (!"COMPLETED".equalsIgnoreCase(task.getDonation().getStatus())) {
                    throw new BadRequestException("Donation delivery tasks can only be completed via verified NGO receipt confirmation.");
                }
            }
            task.setCompletedAt(LocalDateTime.now());
            // When an eligible paid task is completed, transition payment status to PENDING
            if (Boolean.TRUE.equals(task.getIsPaid())) {
                task.setPaymentStatus("PENDING");
            } else {
                task.setPaymentStatus("NOT_APPLICABLE");
            }
        }

        task = taskRepository.save(task);

        // Send notifications to assigner
        notificationService.sendNotification(
                task.getAssignedByUser().getId(),
                "Task Status Updated: " + statusUpper,
                "Volunteer " + task.getVolunteer().getUser().getName() + " updated task '" + task.getTitle() + "' status to " + statusUpper + ".",
                "VOLUNTEER_TASK_UPDATED",
                "VOLUNTEER_TASK",
                task.getId()
        );

        return mapToResponse(task);
    }

    @Override
    @Transactional
    public VolunteerTaskResponse confirmPayment(Long taskId, Long userId) {
        VolunteerTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAssignedVolunteer = task.getVolunteer() != null && task.getVolunteer().getUser().getId().equals(userId);
        boolean isAssigner = task.getAssignedByUser() != null && task.getAssignedByUser().getId().equals(userId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (isAssignedVolunteer) {
            throw new ForbiddenException("Volunteers cannot mark their own tasks as paid.");
        }

        if (!isAssigner && !isAdmin) {
            throw new ForbiddenException("Only the assigning NGO/Restaurant or Admin can confirm task payments.");
        }

        if (!Boolean.TRUE.equals(task.getIsPaid())) {
            throw new BadRequestException("Cannot confirm payment for an unpaid task.");
        }

        if (!"COMPLETED".equalsIgnoreCase(task.getStatus())) {
            throw new BadRequestException("Payment can only be confirmed for COMPLETED tasks.");
        }

        if ("PAID".equalsIgnoreCase(task.getPaymentStatus())) {
            throw new BadRequestException("Payment has already been confirmed as PAID.");
        }

        if ("NOT_APPLICABLE".equalsIgnoreCase(task.getPaymentStatus())) {
            throw new BadRequestException("Payment status is NOT_APPLICABLE. Task must be verified COMPLETED first.");
        }

        task.setPaymentStatus("PAID");
        task.setPaidAt(LocalDateTime.now());
        task.setPaidByUserId(userId);
        task = taskRepository.save(task);

        // Notify volunteer
        notificationService.sendNotification(
                task.getVolunteer().getUser().getId(),
                "Task Payment Confirmed",
                "Payment of " + task.getCurrency() + " " + task.getPaymentAmount() + " for task '" + task.getTitle() + "' has been confirmed by " + currentUser.getName() + ".",
                "TASK_PAYMENT_CONFIRMED",
                "VOLUNTEER_TASK",
                task.getId()
        );

        return mapToResponse(task);
    }

    @Override
    @Transactional
    public VolunteerTaskResponse disputePayment(Long taskId, Long userId, String reason) {
        VolunteerTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAssignedVolunteer = task.getVolunteer() != null && task.getVolunteer().getUser().getId().equals(userId);
        boolean isAssigner = task.getAssignedByUser() != null && task.getAssignedByUser().getId().equals(userId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (!isAssignedVolunteer && !isAssigner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to dispute payment for this task.");
        }

        if (!Boolean.TRUE.equals(task.getIsPaid())) {
            throw new BadRequestException("Cannot dispute payment for an unpaid task.");
        }

        if (!"COMPLETED".equalsIgnoreCase(task.getStatus())) {
            throw new BadRequestException("Payment can only be disputed for COMPLETED tasks.");
        }

        if ("PAID".equalsIgnoreCase(task.getPaymentStatus())) {
            throw new BadRequestException("Cannot dispute an already PAID task.");
        }

        if (reason == null || reason.trim().isBlank()) {
            throw new BadRequestException("Dispute reason is required.");
        }

        task.setPaymentStatus("DISPUTED");
        task.setDisputedAt(LocalDateTime.now());
        task.setDisputeReason(reason.trim());
        task = taskRepository.save(task);

        // Notify other party
        if (isAssignedVolunteer) {
            notificationService.sendNotification(
                    task.getAssignedByUser().getId(),
                    "Volunteer Payment Issue Reported",
                    "Volunteer " + task.getVolunteer().getUser().getName() + " reported a payment dispute for task '" + task.getTitle() + "'. Reason: " + reason.trim(),
                    "TASK_PAYMENT_DISPUTED",
                    "VOLUNTEER_TASK",
                    task.getId()
            );
        } else {
            notificationService.sendNotification(
                    task.getVolunteer().getUser().getId(),
                    "Task Payment Disputed",
                    "Payment for task '" + task.getTitle() + "' has been marked as DISPUTED. Reason: " + reason.trim(),
                    "TASK_PAYMENT_DISPUTED",
                    "VOLUNTEER_TASK",
                    task.getId()
            );
        }

        return mapToResponse(task);
    }

    @Override
    @Transactional
    public VolunteerTaskResponse resolveDispute(Long taskId, Long adminUserId, String resolution, String remarks) {
        VolunteerTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + adminUserId));

        if (!"ADMIN".equalsIgnoreCase(adminUser.getRole().getName())) {
            throw new ForbiddenException("Only Admins can resolve payment disputes.");
        }

        if (!Boolean.TRUE.equals(task.getIsPaid())) {
            throw new BadRequestException("Cannot resolve dispute for an unpaid task.");
        }

        String resolutionUpper = resolution != null ? resolution.trim().toUpperCase() : "RESOLVED";

        if ("PAID".equalsIgnoreCase(resolutionUpper)) {
            task.setPaymentStatus("PAID");
            task.setPaidAt(LocalDateTime.now());
            task.setPaidByUserId(adminUserId);
        } else if ("CANCELLED".equalsIgnoreCase(resolutionUpper) || "REJECTED".equalsIgnoreCase(resolutionUpper)) {
            task.setPaymentStatus("NOT_APPLICABLE");
        } else {
            task.setPaymentStatus("RESOLVED");
        }

        String disputeRemarks = remarks != null && !remarks.isBlank() ? remarks.trim() : "Admin resolution: " + resolutionUpper;
        task.setDisputeReason((task.getDisputeReason() != null ? task.getDisputeReason() + " | " : "") + disputeRemarks);
        task = taskRepository.save(task);

        // Notify both volunteer and assigner
        notificationService.sendNotification(
                task.getVolunteer().getUser().getId(),
                "Dispute Resolved",
                "Payment dispute for task '" + task.getTitle() + "' was resolved by Admin (" + resolutionUpper + "). " + disputeRemarks,
                "TASK_PAYMENT_RESOLVED",
                "VOLUNTEER_TASK",
                task.getId()
        );

        notificationService.sendNotification(
                task.getAssignedByUser().getId(),
                "Dispute Resolved",
                "Payment dispute for task '" + task.getTitle() + "' was resolved by Admin (" + resolutionUpper + "). " + disputeRemarks,
                "TASK_PAYMENT_RESOLVED",
                "VOLUNTEER_TASK",
                task.getId()
        );

        return mapToResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public VolunteerEarningsResponse getVolunteerEarnings(Long volunteerUserId) {
        Volunteer volunteer = volunteerRepository.findByUserId(volunteerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found for user id: " + volunteerUserId));

        List<VolunteerTask> allTasks = taskRepository.findByVolunteerId(volunteer.getId());

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalDisputed = BigDecimal.ZERO;

        List<VolunteerTaskResponse> paidList = new ArrayList<>();
        List<VolunteerTaskResponse> pendingList = new ArrayList<>();
        List<VolunteerTaskResponse> disputedList = new ArrayList<>();
        List<VolunteerTaskResponse> allPaidList = new ArrayList<>();

        for (VolunteerTask t : allTasks) {
            if (Boolean.TRUE.equals(t.getIsPaid())) {
                VolunteerTaskResponse resp = mapToResponse(t);
                allPaidList.add(resp);

                BigDecimal amount = t.getPaymentAmount() != null ? t.getPaymentAmount() : BigDecimal.ZERO;
                String pStatus = t.getPaymentStatus() != null ? t.getPaymentStatus() : "NOT_APPLICABLE";

                if ("PAID".equalsIgnoreCase(pStatus)) {
                    totalPaid = totalPaid.add(amount);
                    paidList.add(resp);
                } else if ("PENDING".equalsIgnoreCase(pStatus)) {
                    totalPending = totalPending.add(amount);
                    pendingList.add(resp);
                } else if ("DISPUTED".equalsIgnoreCase(pStatus)) {
                    totalDisputed = totalDisputed.add(amount);
                    disputedList.add(resp);
                }
            }
        }

        return VolunteerEarningsResponse.builder()
                .totalEarnings(totalPaid)
                .pendingEarnings(totalPending)
                .disputedEarnings(totalDisputed)
                .currency("INR")
                .paidTasksCount(paidList.size())
                .pendingTasksCount(pendingList.size())
                .disputedTasksCount(disputedList.size())
                .paidTasks(paidList)
                .pendingTasks(pendingList)
                .disputedTasks(disputedList)
                .allPaidTasks(allPaidList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerTaskResponse> getDisputedTasks() {
        return taskRepository.findByPaymentStatusOrderByUpdatedAtDesc("DISPUTED").stream()
                .map(this::mapToResponse)
                .toList();
    }

    private VolunteerTaskResponse mapToResponse(VolunteerTask t) {
        return VolunteerTaskResponse.fromEntity(t);
    }
}
