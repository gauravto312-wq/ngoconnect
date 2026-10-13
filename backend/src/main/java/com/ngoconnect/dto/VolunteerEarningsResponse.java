package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerEarningsResponse {
    private BigDecimal totalEarnings;      // sum of PAID amounts
    private BigDecimal pendingEarnings;    // sum of PENDING amounts
    private BigDecimal disputedEarnings;   // sum of DISPUTED amounts
    private String currency;               // "INR"
    private int paidTasksCount;
    private int pendingTasksCount;
    private int disputedTasksCount;
    private List<VolunteerTaskResponse> paidTasks;
    private List<VolunteerTaskResponse> pendingTasks;
    private List<VolunteerTaskResponse> disputedTasks;
    private List<VolunteerTaskResponse> allPaidTasks; // all tasks where isPaid == true
}
