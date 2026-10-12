package com.example.leaderboard.controller;

import com.example.leaderboard.dto.BatchJobRequest;
import com.example.leaderboard.dto.BatchJobResponse;
import com.example.leaderboard.exception.BatchJobNotFoundException;
import com.example.leaderboard.service.BatchJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/batch-jobs")
public class BatchJobController {
    private final BatchJobService service;

    public BatchJobController(BatchJobService service) {
        this.service = service;
    }

    @PostMapping
    public BatchJobResponse submit(@Valid @RequestBody BatchJobRequest request) {
        return service.submit(request.values()).response();
    }

    @GetMapping("/{batchId}")
    public BatchJobResponse get(@PathVariable @Pattern(regexp = "[a-f0-9-]{36}") String batchId) {
        BatchJobService.JobStatus status = service.get(batchId);
        if (status == null) {
            throw new BatchJobNotFoundException(batchId);
        }
        return status.response();
    }
}
