package cn.gentlewind.domain.strategy.service.raffle;

import cn.gentlewind.domain.strategy.model.entity.RaffleAwardEntity;
import cn.gentlewind.domain.strategy.model.entity.RaffleFactorEntity;
import cn.gentlewind.domain.strategy.model.entity.RuleActionEntity;
import cn.gentlewind.domain.strategy.model.entity.StrategyEntity;
import cn.gentlewind.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.gentlewind.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.domain.strategy.service.IRaffleStrategy;
import cn.gentlewind.domain.strategy.service.armory.IStrategyDispatch;
import cn.gentlewind.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.gentlewind.types.enums.ResponseCode;
import cn.gentlewind.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * 抽奖抽象类
 *
 * 使用了模板模式的设计模式，这种设计方式允许不同的子类提供不同的规则过滤逻辑，而无需改变抽奖流程的基本结构。
 * 这种方式使得代码更加灵活，易于扩展和维护。当需要添加新的抽奖策略时，只需要添加一个新的子类，并实现相应的规则过滤逻辑即可。
 */
@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    // 策略仓储服务 -> domain层像一个大厨，仓储层提供米面粮油
    protected IStrategyRepository repository;
    // 策略调度服务 -> 只负责抽奖处理，通过新增接口的方式，隔离职责，不需要使用方关心或者调用抽奖的初始化
    protected IStrategyDispatch strategyDispatch;

    // 构造函数
    public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
    }

    /**
     * 定义了一个模板方法performRaffle，它包含了抽奖过程的基本步骤
     *
     * @param raffleFactorEntity 抽奖因子实体对象，根据入参信息计算抽奖结果，包含用户id和策略id
     * @return
     */
    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 1. 参数校验
        String userId = raffleFactorEntity.getUserId();
        Long strategyId = raffleFactorEntity.getStrategyId();
        if (null == strategyId || StringUtils.isBlank(userId)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 2. 查询抽奖策略4000,5000,6000
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);

        // 3. 抽奖前 - 规则过滤
        // 拿到规则过滤的规则，即用户id，策略id，奖品id
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleActionEntity = this
                .doCheckRaffleBeforeLogic(RaffleFactorEntity.builder().userId(userId).strategyId(strategyId).build(), strategy.ruleModels());

        //  ruleActionEntity 的 code 属性是否表示需要规则引擎接管后续流程。如果相等，则执行特定的逻辑，否则继续按照默认流程进行
        if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionEntity.getCode())) {

            if (DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode().equals(ruleActionEntity.getRuleModel())) {
                // 黑名单返回固定的奖品ID
                return RaffleAwardEntity.builder()
                        .awardId(ruleActionEntity.getData().getAwardId())
                        .build();
            } else if (DefaultLogicFactory.LogicModel.RULE_WIGHT.getCode().equals(ruleActionEntity.getRuleModel())) {
                // 权重根据返回的信息进行抽奖
                RuleActionEntity.RaffleBeforeEntity raffleBeforeEntity = ruleActionEntity.getData();
                String ruleWeightValueKey = raffleBeforeEntity.getRuleWeightValueKey();
                Integer awardId = strategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey);
                return RaffleAwardEntity.builder()
                        .awardId(awardId)
                        .build();
            }
        }

        // 4. 默认抽奖流程
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId);

        // 5. 查询奖品规则：抽奖中（拿到奖品ID时，过滤规则）、抽奖后（扣减完奖品库存后过滤，抽奖中拦截和无库存则走兜底）
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModelVO(strategyId, awardId);

        // 6. 抽奖中-规则过滤
        RuleActionEntity<RuleActionEntity.RaffleCenterEntity> ruleActionCenterEntity = this.doCheckRaffleCenterLogic(RaffleFactorEntity.builder()
                .userId(userId)
                .strategyId(strategyId)
                .awardId(awardId)
                .build(), strategyAwardRuleModelVO.raffleCenterRuleModelList());

        if(RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(ruleActionCenterEntity.getCode())){
            log.info("【临时日志】中奖中规则拦截，通过抽奖后规则rule_luck_award 走兜底奖励。");
            return RaffleAwardEntity. builder()
                    .awardDesc("中奖中规则拦截，通过抽奖后规则rule_luck_award 走兜底奖励。")
                    .build();
        }


        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .build();
    }

    // 定义了一个抽象方法doCheckRaffleBeforeLogic，它由子类实现，用于实现抽奖前的规则过滤
    protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity, String... logics);

    protected abstract RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity, String... logics);

//    protected abstract RuleActionEntity<RuleActionEntity.RaffleAfterEntity> doCheckRaffleAfterLogic(RaffleFactorEntity raffleFactorEntity, String... logics);
}