package cn.gentlewind.domain.strategy.service.rule.impl;

import cn.gentlewind.domain.strategy.model.entity.RuleActionEntity;
import cn.gentlewind.domain.strategy.model.entity.RuleMatterEntity;
import cn.gentlewind.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.domain.strategy.service.annotation.LogicStrategy;
import cn.gentlewind.domain.strategy.service.rule.ILogicFilter;
import cn.gentlewind.domain.strategy.service.rule.factory.DefaultLogicFactory;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 抽奖中置规则过滤
 *
 * 次数校验
 */
@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_LOCK)
public class RuleLockLogicFilter implements ILogicFilter<RuleActionEntity.RaffleCenterEntity> {

    @Resource
    private IStrategyRepository repository;

    // 定义用户目前的抽奖次数，后续从数据库查
    private Long userRaffleCount = 0L;


    @Override
    public RuleActionEntity<RuleActionEntity.RaffleCenterEntity> filter(RuleMatterEntity ruleMatterEntity) {
        // 打日志
        log.info("规则过滤-次数锁： userId：{} strategyId：{} ruleModel：{}",ruleMatterEntity.getUserId(),
                ruleMatterEntity.getStrategyId(),ruleMatterEntity.getRuleModel());

        // 查询规则值的配置 rule_lock:1/2/6 表示抽奖次数为1/2/6次后解锁
        String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(), ruleMatterEntity.getAwardId(), ruleMatterEntity.getRuleModel());

        // 次数为1/2/6
        long raffleCount = Long.parseLong(ruleValue);

        // 规则过滤
        // 当用户抽奖次数大于规则值，则放行
        if(userRaffleCount>= raffleCount){
            return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }

        // 当用户抽奖次数小于规则值,规则拦截
        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .build();

    }
}
