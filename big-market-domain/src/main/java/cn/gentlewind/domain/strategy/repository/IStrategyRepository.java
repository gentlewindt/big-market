package cn.gentlewind.domain.strategy.repository;

import cn.gentlewind.domain.strategy.model.StrategyAwardEntity;
import cn.gentlewind.domain.strategy.model.StrategyEntity;
import cn.gentlewind.domain.strategy.model.StrategyRuleEntity;

import java.util.List;
import java.util.Map;

/**
 * 策略服务仓储接口
 */
public interface IStrategyRepository {

    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);


    void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Long> strategyAwardSearchRateTable);

    Integer getStrategyAwardAssemble(String key, Integer rateKey);

    int getRateRange(Long strategyId);

    int getRateRange(String key);

    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);
}
