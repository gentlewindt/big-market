package cn.gentlewind.domain.strategy.model.entity;

import cn.gentlewind.types.common.Constants;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyEntity {

    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖策略描述 */
    private String strategyDesc;
    /** 抽奖规则模型 rule_weight,rule_blacklist */
    private String ruleModels;

    /**
     * 处理规则模型
     *
     * @return
     */
    public String[] ruleModels() {
        if (StringUtils.isBlank(ruleModels)) return null;
        // 分割为一个字符串数组并返回（rule_weight,rule_blacklist）
        return ruleModels.split(Constants.SPLIT);
    }

    /**
     * 获取规则模型
     *
     * @return
     */
    public String getRuleWeight() {
        // 调用ruleModels方法获取规则模型数组。
        String[] ruleModels = this.ruleModels();
        // 遍历规则模型数组，如果包含"rule_weight"，则返回"rule_weight"。
        for (String ruleModel : ruleModels) {
            if ("rule_weight".equals(ruleModel)) return ruleModel;
        }
        return null;
    }

}