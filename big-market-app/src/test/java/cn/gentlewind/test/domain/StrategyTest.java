package cn.gentlewind.test.domain;

import cn.gentlewind.domain.strategy.service.armory.IStrategyArmory;
import cn.gentlewind.infrastructure.persistent.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class StrategyTest {

    @Resource
    private IStrategyArmory strategyArmory;

    /**
     * 策略ID；100001L、100002L 装配的时候创建策略表写入到 Redis Map 中
     */
    @Test
    public void test_strategyArmory() {
        boolean success = strategyArmory.assembleLotteryStrategy(100001L);
        log.info("测试结果：{}", success);
    }

    /**
     * 从装配的策略中随机获取奖品ID值
     */
    @Test
    public void test_getAssembleRandomVal() {
        log.info("测试结果：{} - 奖品ID值", strategyArmory.getRandomAwardId(100002L));
    }

    @Test
    public void test_shuffle(){
        Map<Integer, Integer> strategyAwardSearchRateTable = new HashMap<>();
        // 添加内容到Map中
        strategyAwardSearchRateTable.put(1, 10);
        strategyAwardSearchRateTable.put(2, 20);
        strategyAwardSearchRateTable.put(3, 30);
        strategyAwardSearchRateTable.put(4, 40);

        // 将Map中的值转换为List
        List<Integer> valueList = new ArrayList<>(strategyAwardSearchRateTable.values());

        // 使用Collections.shuffle()方法对值的List进行乱序
        Collections.shuffle(valueList);

        // 将乱序后的值重新放回Map中
        Map<Integer, Integer> randomizedMap = new LinkedHashMap<>();
        Iterator<Integer> valueIterator = valueList.iterator();
        for (Integer key : strategyAwardSearchRateTable.keySet()) {
            randomizedMap.put(key, valueIterator.next());
        }

        // 打印乱序后的Map内容
        for (Map.Entry<Integer, Integer> entry : randomizedMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }

}
