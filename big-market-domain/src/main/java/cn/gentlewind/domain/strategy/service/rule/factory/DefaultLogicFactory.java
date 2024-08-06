package cn.gentlewind.domain.strategy.service.rule.factory;

import cn.gentlewind.domain.strategy.model.entity.RuleActionEntity;
import cn.gentlewind.domain.strategy.service.annotation.LogicStrategy;
import cn.gentlewind.domain.strategy.service.rule.ILogicFilter;
import com.alibaba.fastjson2.util.AnnotationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description 规则工厂
 *
 * 用于管理和提供不同类型的逻辑过滤器。通过构造函数注入逻辑过滤器列表，
 * 并基于过滤器类上的注解，将它们存储在映射中。枚举LogicModel定义了不同的逻辑模式，便于在代码中使用。
 */
@Service
public class DefaultLogicFactory {

    // 定义一个ConcurrentHashMap，用于存储逻辑过滤器
    public Map<String, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();

    // 构造函数，接收一个ILogicFilter列表
    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
        // 遍历传入的逻辑过滤器列表
        logicFilters.forEach(logic -> {
            // 查找逻辑过滤器类上的LogicStrategy注解
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
            if (null != strategy) {
                // 如果注解存在，将逻辑模式的代码和逻辑过滤器放入逻辑过滤器映射中
                logicFilterMap.put(strategy.logicMode().getCode(), logic);
            }
        });
    }

    // 返回逻辑过滤器映射，使用泛型进行类型转换
    public <T extends RuleActionEntity.RaffleEntity> Map<String, ILogicFilter<T>> openLogicFilter() {
        return (Map<String, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
    }

    // 定义一个枚举类，用于逻辑模型
    @Getter
    @AllArgsConstructor
    public enum LogicModel {
        RULE_WIGHT("rule_weight", "【抽奖前规则】根据抽奖权重返回可抽奖范围KEY", "before"),
        RULE_BLACKLIST("rule_blacklist", "【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回", "before"),
        RULE_LOCK("rule_lock", "【抽奖中规则】抽奖n次后，对应奖品可解锁抽奖", "center"),
        RULE_LUCK_AWARD("rule_luck_award", "【抽奖后规则】抽奖n次后，对应奖品可解锁抽奖", "after"),
        ;

        private final String code;
        private final String info;
        private final String type;

        public static boolean isCenter(String code){
            return "center".equals(LogicModel.valueOf(code.toUpperCase()).type);
        }

        public static boolean isAfter(String code){
            return "after".equals(LogicModel.valueOf(code.toUpperCase()).type);
        }

    }

}
