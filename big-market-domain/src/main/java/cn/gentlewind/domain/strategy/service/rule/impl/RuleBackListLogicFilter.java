package cn.gentlewind.domain.strategy.service.rule.impl;

import cn.gentlewind.domain.strategy.model.entity.RuleActionEntity;
import cn.gentlewind.domain.strategy.model.entity.RuleMatterEntity;
import cn.gentlewind.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.domain.strategy.service.annotation.LogicStrategy;
import cn.gentlewind.domain.strategy.service.rule.ILogicFilter;
import cn.gentlewind.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.gentlewind.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 【抽奖前规则】黑名单用户过滤规则
 */

@Slf4j
@Component
// 指定过滤规则模型为 ：黑名单用户过滤规则
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_BLACKLIST)
public class RuleBackListLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    // 提供基础服务
    @Resource
    private IStrategyRepository repository;

    /**
     * 执行过滤
     *
     * @param ruleMatterEntity 规则实体,用于过滤规则的必要参数信息。
     * @return
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-黑名单 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());

        // 从ruleMatterEntity对象中获取用户ID。ruleMatterEntity是一个包含策略相关数据的实体。
        String userId = ruleMatterEntity.getUserId();

        // 从存储库中查询规则值配置。使用策略ID、奖项ID和规则模型作为查询参数。
        String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(), ruleMatterEntity.getAwardId(), ruleMatterEntity.getRuleModel());

        // 将查询到的规则值字符串按冒号进行分割，得到一个字符串数组splitRuleValue。
        String[] splitRuleValue = ruleValue.split(Constants.COLON);
        // 从splitRuleValue数组中提取第一个元素，并将其转换为整数类型，表示黑名单用户的固定奖品ID。
        Integer awardId = Integer.parseInt(splitRuleValue[0]);

        // 从splitRuleValue数组中提取第二个元素，并将其按逗号进行分割，得到一个字符串数组userBlackIds。
        String[] userBlackIds = splitRuleValue[1].split(Constants.SPLIT);
        // 遍历黑名单列表的每个黑名单用户id
        for (String userBlackId : userBlackIds) {
            // 如果当前用户id与黑名单中的某个用户id匹配
            if (userId.equals(userBlackId)) {
                // 创建并返回一个RuleActionEntity对象，表明用户被黑名单规则接管
                return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                        // 设置规则模型为黑名单规则
                        .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                        // 设置关联的数据，包括策略id和奖品id
                        .data(RuleActionEntity.RaffleBeforeEntity.builder()
                                .strategyId(ruleMatterEntity.getStrategyId())
                                .awardId(awardId)
                                .build())
                        // 设置状态码为TAKE_OVER，表明规则接管
                        .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                        .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                        // 构建并返回这个RuleActionEntity对象
                        .build();
            }
        }

        // 如果用户ID不在黑名单中，返回一个允许继续的规则动作实体
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                // 设置状态码为ALLOW，表明用户被允许继续操作
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

}