package cn.gentlewind.domain.strategy.repository;

import cn.gentlewind.domain.strategy.model.StrategyAwardEntity;

import java.util.List;
import java.util.Map;

/**
 * 策略服务仓储接口
 */
public interface IStrategyRepository {

    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);


    void storeStrategyAwardSearchRateTable(Long strategyId, Integer rateRange, Map<Integer, Long> strategyAwardSearchRateTable);

    Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey);

    int getRateRange(Long strategyId);
}
