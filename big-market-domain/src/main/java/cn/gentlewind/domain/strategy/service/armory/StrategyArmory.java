package cn.gentlewind.domain.strategy.service.armory;

import cn.gentlewind.domain.strategy.model.StrategyAwardEntity;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;


@Slf4j
@Service
public class StrategyArmory implements IStrategyArmory {

    @Resource
    private IStrategyRepository repository;

    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);

        // 2. 获取最小概率值 minAwardRate
        // 通过流式处理计算`strategyAwardEntities`列表中`awardRate`属性的最小值。
        BigDecimal minAwardRate = strategyAwardEntities.stream() // 将`strategyAwardEntities`列表转换为流。
                .map(StrategyAwardEntity::getAwardRate) // 将每个StrategyAwardEntity对象的awardRate属性转换为BigDecimal类型。
                .min(BigDecimal::compareTo) // 对每个BigDecimal对象执行compareTo进行自然排序，并返回最小值。
                .orElse(BigDecimal.ZERO); // 如果`strategyAwardEntities`列表为空，则返回BigDecimal.ZERO零值
        if(minAwardRate == BigDecimal.valueOf(0)){
            log.info("最小概率为0");
        }


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
        repository.storeStrategyAwardSearchRateTable(strategyId, shuffleStrategyAwardSearchRateTable.size(), shuffleStrategyAwardSearchRateTable);

        return true;
    }

    /**
     * 获取随机奖品ID，也就是抽奖
     *
     * @param strategyId 策略ID：分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取策略id
     * @return
     */
    @Override
    public Integer getRandomAwardId(Long strategyId) {
        int rateRange = repository.getRateRange(strategyId);
        // 通过生成的随机值，获取概率值奖品查找表的结果
        return repository.getStrategyAwardAssemble(strategyId, new SecureRandom().nextInt(rateRange));
        // SecureRandom().nextInt(rateRange): 生成一个随机整数，范围为0到rateRange-1。（不含rateRange）
    }

}
