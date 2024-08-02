package cn.gentlewind.domain.strategy.service.armory;

public interface IStrategyDispatch {

    /**
     * 获取抽奖策略装配的随机结果，也就是抽奖
     *
     * @param strategyId 策略ID
     * @return 抽奖结果
     */
    Integer getRandomAwardId(Long strategyId);

    Integer getRandomAwardId(Long strategyId, String ruleWeightValue);
}
