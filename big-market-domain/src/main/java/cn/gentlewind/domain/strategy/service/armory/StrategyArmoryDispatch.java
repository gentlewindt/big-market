package cn.gentlewind.domain.strategy.service.armory;

import cn.gentlewind.domain.strategy.model.StrategyAwardEntity;
import cn.gentlewind.domain.strategy.model.StrategyEntity;
import cn.gentlewind.domain.strategy.model.StrategyRuleEntity;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.types.enums.ResponseCode;
import cn.gentlewind.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;



@Service
public class StrategyArmoryDispatch implements IStrategyArmory ,IStrategyDispatch{

    @Resource
    private IStrategyRepository repository;

    /**
     * 重新构建策略奖品概率查找表
     *
     * 根据不同的策略和权重配置，生成并存储不同的抽奖策略查找表
     *
     * @param strategyId 策略ID
     * @return
     */
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 查询策略配置
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);

        // 构建策略奖品概率查找表
        assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntities);

        // 查询策略实体类
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);

        // 取出策略规则（rule_weight,rule_blacklist）
        String ruleWeight = strategyEntity.getRuleWeight();
        if (null == ruleWeight) return true;

        // 通过策略ID和rule_weight查询策略规则，拿到策略规则实体类
        StrategyRuleEntity strategyRuleEntity = repository.queryStrategyRule(strategyId, ruleWeight);
        if (null == strategyRuleEntity){
            // 如果策略规则为空，则抛出异常
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(), ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo()) ;
        }

        // 获取权重值
        // 4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
        Map<String, List<Integer>> ruleWeightValueMap = strategyRuleEntity.getRuleWeightValues();
        // 提取所有的key，放入集合set中
        Set<String> keys = ruleWeightValueMap.keySet();
        // 遍历所有的权重规则
        for (String key : keys) {
            // 获取对应的权重值
            List<Integer> ruleWeightValues = ruleWeightValueMap.get(key);
            // 克隆一份策略奖品列表
            ArrayList<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardEntities);
            // 移除掉不在规则值中的奖品
            // entity -> !ruleWeightValues.contains(entity.getAwardId())
            // lambda表达式，接受一个StrategyAwardEntity类型的entity，判断是否在ruleWeightValues中，如果不在则返回true，表示需要移除该元素。
            strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
            // 重新构建策略奖品概率查找表
            assembleLotteryStrategy(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
        }

        return true;



    }


    /**
     * 根据策略和权值装配抽奖策略
     *
     * @param key                   策略标识符字符串
     * @param strategyAwardEntities 包含奖品和对应概率的实体列表
     */
    private void assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardEntities) {
        // 2. 获取最小概率值 minAwardRate
        // 通过流式处理计算`strategyAwardEntities`列表中`awardRate`属性的最小值。
        BigDecimal minAwardRate = strategyAwardEntities.stream() // 将`strategyAwardEntities`列表转换为流。
                .map(StrategyAwardEntity::getAwardRate) // 将每个StrategyAwardEntity对象的awardRate属性转换为BigDecimal类型。
                .min(BigDecimal::compareTo) // 对每个BigDecimal对象执行compareTo进行自然排序，并返回最小值。
                .orElse(BigDecimal.ZERO); // 如果`strategyAwardEntities`列表为空，则返回BigDecimal.ZERO零值


        // 3. 获取概率值总和 totalAwardRate
        BigDecimal totalAwardRate = strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        1. `reduce`是一个终端操作，用于将流中的元素聚合起来，产生单一的结果。
//        2. 第一个参数`BigDecimal.ZERO`作为初始值，确保在流为空的情况下也能正常工作。
//        3. 第二个参数`BigDecimal::add`是一个方法引用，表示使用`BigDecimal`类的`add`方法作为累加器函数。
//        - 这意味着流中的每个元素都将被依次与累积结果相加。
//        - 初始累积结果为`BigDecimal.ZERO`，之后每次迭代都会更新累积结果。
//        reduce`操作最终返回流中所有`BigDecimal`元素的总和


        // 4. 用总概率除以最小概率拿到概率的范围（百分位、千分位、万分位）
        // 比如：用 1 / 0.0001 获得概率范围为10000，0.95 / 0.0001
        BigDecimal rateRange = totalAwardRate.divide(minAwardRate, 0, RoundingMode.CEILING);
//        divide：除法运算
//        - 除数：`minAwardRate`。
//        - 小数位数：`0`，意味着结果将是一个整数。
//        - 四舍五入模式：`RoundingMode.CEILING`，表示结果将向上取整。floor：向下取整

        // 5. 生成策略奖品概率查找表「这里指需要在list集合中，存放上对应的奖品占位即可，占位越多等于概率越高」这种方法允许通过简单地随机选择列表中的一个元素来实现概率权重的选择机制。
        // 预定义一个查找表
        List<Long> strategyAwardSearchRateTables = new ArrayList<>(rateRange.intValue()); // bigDecimal方法，转为int类型
        for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
            Long awardId = strategyAward.getAwardId();
            BigDecimal awardRate = strategyAward.getAwardRate();
            // 计算出每个概率值需要存放到查找表的数量，循环填充
            // 通过计算 rateRange 和 awardRate 的乘积来确定每个奖项在查找表中应该出现的次数。rateRange.multiply(awardRate) 返回一个 BigDecimal，
            // 然后通过 setScale(0, RoundingMode.CEILING) 将其向上取整，并转换为 int 类型。这个值表示当前奖项 awardId 在查找表中应出现的次数。
            // 随后，通过循环将 awardId 多次添加到 strategyAwardSearchRateTables 列表中。
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchRateTables.add(awardId);
            }
        }

        // 6. 对存储的奖品进行乱序操作
        Collections.shuffle(strategyAwardSearchRateTables);
        // shuffle：对列表进行随机排列

        // 7. 将查找表添加到 Map 中，并且将索引作为键，奖品 ID 作为值存储到 Map 中。
        // 这样能够通过索引来获取对应的奖品 ID。
        Map<Integer, Long> shuffleStrategyAwardSearchRateTable = new LinkedHashMap<>();
        for (int i = 0; i < strategyAwardSearchRateTables.size(); i++) {
            shuffleStrategyAwardSearchRateTable.put(i, strategyAwardSearchRateTables.get(i));
        }

        // 8. 将查找表存放到 Redis
        repository.storeStrategyAwardSearchRateTable(key, shuffleStrategyAwardSearchRateTable.size(), shuffleStrategyAwardSearchRateTable);

    }
    @Override
    public Integer getRandomAwardId(Long strategyId) {
        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
        int rateRange = repository.getRateRange(strategyId);
        // 通过生成的随机值，获取概率值奖品查找表的结果
        return repository.getStrategyAwardAssemble(String.valueOf(strategyId), new SecureRandom().nextInt(rateRange));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        String key = String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
        int rateRange = repository.getRateRange(key);
        // 通过生成的随机值，获取概率值奖品查找表的结果
        return repository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
    }

    /**
     * 获取随机奖品ID，也就是抽奖
     *
     * @param strategyId 策略ID：分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取策略id
     * @return
     */
//    @Override
//    public Integer getRandomAwardId(Long strategyId) {
//        int rateRange = repository.getRateRange(strategyId);
//        // 通过生成的随机值，获取概率值奖品查找表的结果
//        return repository.getStrategyAwardAssemble(strategyId, new SecureRandom().nextInt(rateRange));
//        // SecureRandom().nextInt(rateRange): 生成一个随机整数，范围为0到rateRange-1。（不含rateRange）
//    }

}
