package com.ngoconnect.service;

import com.ngoconnect.dto.VolunteerEarningsResponse;
import com.ngoconnect.dto.VolunteerTaskRequest;
import com.ngoconnect.dto.VolunteerTaskResponse;
import java.util.List;

public interface TaskService {
    VolunteerTaskResponse assignTask(Long assignedByUserId, VolunteerTaskRequest request);
    List<VolunteerTaskResponse> getTasksForVolunteer(Long volunteerUserId);
    List<VolunteerTaskResponse> getTasksAssignedByUser(Long userId);
    VolunteerTaskResponse updateTaskStatus(Long taskId, String newStatus, Long userId);
    VolunteerTaskResponse confirmPayment(Long taskId, Long userId);
    VolunteerTaskResponse disputePayment(Long taskId, Long userId, String reason);
    VolunteerTaskResponse resolveDispute(Long taskId, Long adminUserId, String resolution, String remarks);
    VolunteerEarningsResponse getVolunteerEarnings(Long volunteerUserId);
    List<VolunteerTaskResponse> getDisputedTasks();
}
