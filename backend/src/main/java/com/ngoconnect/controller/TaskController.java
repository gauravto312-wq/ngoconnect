package com.ngoconnect.controller;

import com.ngoconnect.dto.TaskDisputeRequest;
import com.ngoconnect.dto.VolunteerEarningsResponse;
import com.ngoconnect.dto.VolunteerTaskRequest;
import com.ngoconnect.dto.VolunteerTaskResponse;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('NGO', 'RESTAURANT', 'ADMIN')")
    public ResponseEntity<VolunteerTaskResponse> assignTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody VolunteerTaskRequest request) {
        return new ResponseEntity<>(taskService.assignTask(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/my-volunteer-tasks")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<VolunteerTaskResponse>> getMyVolunteerTasks(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.getTasksForVolunteer(currentUser.getId()));
    }

    @GetMapping("/assigned-by-me")
    public ResponseEntity<List<VolunteerTaskResponse>> getTasksAssignedByMe(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.getTasksAssignedByUser(currentUser.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VolunteerTaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String status = body.get("status");
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status, currentUser.getId()));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VolunteerTaskResponse> confirmPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.confirmPayment(id, currentUser.getId()));
    }

    @PostMapping("/{id}/dispute-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VolunteerTaskResponse> disputePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String reason = body.get("reason");
        return ResponseEntity.ok(taskService.disputePayment(id, currentUser.getId(), reason));
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerEarningsResponse> getEarnings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.getVolunteerEarnings(currentUser.getId()));
    }

    @GetMapping("/disputed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VolunteerTaskResponse>> getDisputedTasks() {
        return ResponseEntity.ok(taskService.getDisputedTasks());
    }

    @PostMapping("/{id}/resolve-dispute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VolunteerTaskResponse> resolveDispute(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String resolution = body.get("resolution");
        String remarks = body.get("remarks");
        return ResponseEntity.ok(taskService.resolveDispute(id, currentUser.getId(), resolution, remarks));
    }
}
