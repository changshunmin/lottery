package com.lottery.controller;

import com.lottery.model.LotteryResult;
import com.lottery.repository.LotteryResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lottery")
public class LotteryController {
    private final LotteryResultRepository repository;

    @Autowired
    public LotteryController(LotteryResultRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/submit")
    public LotteryResult submitResult(@RequestBody LotteryResult result) {
        result.setCreatedAt(LocalDateTime.now());
        return repository.save(result);
    }

    @GetMapping("/all")
    public List<LotteryResult> getAllResults() {
        return repository.findAll();
    }

    @PostMapping("/{id}/claim")
    public LotteryResult claimPrize(@PathVariable Long id, @RequestBody Map<String, String> request) {
        LotteryResult result = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Result not found"));
        if (result.getClaimedBy() != null) {
            throw new RuntimeException("Prize already claimed");
        }
        result.setClaimedBy(request.get("claimerName"));
        result.setClaimedAt(LocalDateTime.now());
        return repository.save(result);
    }
}