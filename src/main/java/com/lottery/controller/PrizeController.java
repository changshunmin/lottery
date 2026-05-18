package com.lottery.controller;

import com.lottery.model.PrizeConfig;
import com.lottery.repository.PrizeConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/prizes")
public class PrizeController {

    private final PrizeConfigRepository repository;

    @Autowired
    public PrizeController(PrizeConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PrizeConfig> getAllPrizes() {
        return repository.findAllByOrderBySortOrderAsc();
    }

    @PostMapping("/batch")
    public List<PrizeConfig> batchSave(@RequestBody List<PrizeConfig> prizes) {
        repository.deleteAll();
        for (int i = 0; i < prizes.size(); i++) {
            PrizeConfig p = prizes.get(i);
            p.setId(null);
            p.setSortOrder(i);
        }
        return repository.saveAll(prizes);
    }

    @PostConstruct
    public void initDefaults() {
        if (repository.count() > 0) return;

        List<PrizeConfig> defaults = Arrays.asList(
            new PrizeConfig("奖品随机选", "❓", 0.5, 0),
            new PrizeConfig("笔记本电脑", "💻", 1.0, 1),
            new PrizeConfig("谢谢惠顾", "😊", 38.25, 2),
            new PrizeConfig("手机", "📱", 2.0, 3),
            new PrizeConfig("充电宝", "🔋", 5.0, 4),
            new PrizeConfig("再抽一次", "🔄", 5.0, 5),
            new PrizeConfig("汇演发票", "🧾", 10.0, 6),
            new PrizeConfig("谢谢惠顾", "😊", 38.25, 7)
        );
        repository.saveAll(defaults);
    }
}
