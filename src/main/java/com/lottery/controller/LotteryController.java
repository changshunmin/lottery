package com.lottery.controller;

import com.lottery.dto.DrawResponse;
import com.lottery.dto.PageResponse;
import com.lottery.model.LotteryResult;
import com.lottery.repository.LotteryResultRepository;
import com.lottery.service.LotteryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lottery")
public class LotteryController {
    private final LotteryResultRepository repository;
    private final LotteryService lotteryService;

    @Autowired
    public LotteryController(LotteryResultRepository repository, LotteryService lotteryService) {
        this.repository = repository;
        this.lotteryService = lotteryService;
    }

    @PostMapping("/draw")
    public DrawResponse draw(@RequestBody Map<String, String> request) {
        return lotteryService.draw(request.get("userName"));
    }

    @GetMapping("/recent")
    public List<LotteryResult> getRecentResults(@RequestParam(defaultValue = "20") int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        return repository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    @GetMapping("/page")
    public PageResponse<LotteryResult> getPagedResults(
            @RequestParam(required = false) String prize,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String prizeFilter = emptyToNull(prize);
        String userFilter = emptyToNull(user);
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(23, 59, 59);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<LotteryResult> result = repository.searchPaged(
                prizeFilter, userFilter, start, end,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                safePage,
                safeSize);
    }

    @PostMapping("/{id}/claim")
    @Transactional
    public LotteryResult claimPrize(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String claimerName = request.get("claimerName");
        if (claimerName == null || claimerName.trim().isEmpty()) {
            throw new RuntimeException("请输入领奖人姓名");
        }
        int updated = repository.claimIfUnclaimed(id, claimerName.trim(), LocalDateTime.now());
        if (updated == 0) {
            if (!repository.existsById(id)) {
                throw new RuntimeException("Result not found");
            }
            throw new RuntimeException("Prize already claimed");
        }
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Result not found"));
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
