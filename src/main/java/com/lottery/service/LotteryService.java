package com.lottery.service;

import com.lottery.dto.DrawResponse;
import com.lottery.model.LotteryResult;
import com.lottery.model.PrizeConfig;
import com.lottery.repository.LotteryResultRepository;
import com.lottery.repository.PrizeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LotteryService {

    private final PrizeConfigRepository prizeRepository;
    private final LotteryResultRepository resultRepository;

    public LotteryService(PrizeConfigRepository prizeRepository,
                          LotteryResultRepository resultRepository) {
        this.prizeRepository = prizeRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public DrawResponse draw(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入您的名字");
        }
        String name = userName.trim();

        List<PrizeConfig> prizes = prizeRepository.findAllByOrderBySortOrderAsc();
        if (prizes.isEmpty()) {
            throw new IllegalStateException("奖品配置为空");
        }

        int index = pickWeightedIndex(prizes);
        PrizeConfig prize = prizes.get(index);

        LotteryResult result = new LotteryResult(name + " - " + prize.getName());
        result.setCreatedAt(LocalDateTime.now());
        result = resultRepository.save(result);

        return new DrawResponse(
                result.getId(),
                prize.getName(),
                prize.getIcon(),
                index,
                name,
                result.getCreatedAt()
        );
    }

    private int pickWeightedIndex(List<PrizeConfig> prizes) {
        double total = 0.0;
        for (PrizeConfig p : prizes) {
            total += p.getProbability() == null ? 0.0 : p.getProbability();
        }
        if (total <= 0) {
            return ThreadLocalRandom.current().nextInt(prizes.size());
        }
        double r = ThreadLocalRandom.current().nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < prizes.size(); i++) {
            double prob = prizes.get(i).getProbability() == null ? 0.0 : prizes.get(i).getProbability();
            cumulative += prob;
            if (r < cumulative) {
                return i;
            }
        }
        return prizes.size() - 1;
    }
}
