package cn.gentlewind.domain.strategy.service.rule;
import cn.gentlewind.domain.strategy.model.entity.RuleActionEntity;
import cn.gentlewind.domain.strategy.model.entity.RuleMatterEntity;

public interface ILogicFilter<T extends RuleActionEntity.RaffleEntity> {

    RuleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);

}