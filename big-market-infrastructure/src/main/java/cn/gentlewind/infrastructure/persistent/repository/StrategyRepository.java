package cn.gentlewind.infrastructure.persistent.repository;

import cn.gentlewind.domain.strategy.model.StrategyAwardEntity;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.gentlewind.infrastructure.persistent.po.StrategyAwardPO;
import cn.gentlewind.infrastructure.persistent.redis.IRedisService;
import cn.gentlewind.types.common.Constants;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
public class StrategyRepository implements IStrategyRepository {

    // 操作数据库对象
    @Resource
    private IStrategyAwardDao strategyAwardDao;

    // 使用Redis
    @Resource
    private IRedisService redisService;

    /**
     * 查询策略的奖品列表
     *
     * 实现了缓存优先的查询策略，首先尝试从Redis缓存中获取`StrategyAwardEntity`列表，
     * 若缓存中不存在，则从数据库中查询数据并转换为`StrategyAwardEntity`对象列表，
     * 再将结果存储至缓存中，最后返回查询结果.
     * @param strategyId
     * @return strategyAwardEntities
     */
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {

        // STRATEGY_AWARD_KEY是固定的字符串前缀，strategyId是动态传入的策略ID，拼接后的字符串作为缓存的唯一标识。
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;

        // 取出缓存中的数据
        List<StrategyAwardEntity> strategyAwardEntities = redisService.getValue(cacheKey);
        // 如果不为空
        if(strategyAwardEntities != null && !strategyAwardEntities.isEmpty()){
            return strategyAwardEntities;
        }

        // 如果为空，那就从数据库中取出数据
        // 从数据库查询与特定`strategyId`相关的所有`StrategyAwardPO`对象，并转换成`StrategyAwardEntity`对象列表。
       List<StrategyAwardPO> strategyAwardPOS = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        // 初始化一个新的`ArrayList`，预设大小为`StrategyAwardPO`列表的长度，提高性能。
        strategyAwardEntities = new ArrayList<>(strategyAwardPOS.size());
        // 遍历`StrategyAwardPO`列表，将每个`StrategyAwardPO`对象转换为`StrategyAwardEntity`对象，并添加到`strategyAwardEntities`列表中。
        for(StrategyAwardPO strategyAwardPO : strategyAwardPOS){
            StrategyAwardEntity strategyAwardEntity = StrategyAwardEntity.builder()
                    .strategyId(strategyAwardPO.getStrategyId())
                    .awardId(strategyAwardPO.getAwardId())
                    .awardCount(strategyAwardPO.getAwardCount())
                    .awardCountSurplus(strategyAwardPO.getAwardCountSurplus())
                    .awardRate(strategyAwardPO.getAwardRate())
                    .build();
            // 将每个构建好的`StrategyAwardEntity`添加到`strategyAwardEntities`列表中。
            strategyAwardEntities.add(strategyAwardEntity);
            // strategyAwardEntities`列表包含了从数据库查询并转换的所有`StrategyAwardEntity`对象。
        }

        // 将`strategyAwardEntities`列表存储到Redis中，以`cacheKey`作为键，随后返回该列表。
        redisService.setValue(cacheKey, strategyAwardEntities);
        return  strategyAwardEntities;
    }

    /**
     * 用于将抽奖策略的范围值和概率查找表存储到Redis缓存中
     *
     * @param strategyId
     * @param rateRange
     * @param strategyAwardSearchRateTable
     */
    @Override
    public void storeStrategyAwardSearchRateTable(Long strategyId, Integer rateRange, Map<Integer, Long> strategyAwardSearchRateTable) {
        // 1. 存储抽奖策略范围值，如10000
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + strategyId, rateRange);
        // 2. 存储概率查找表
        // 从获取或创建一个map
        Map<Integer,Long> cacheRateTable = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId);
        // 将value存入map
        cacheRateTable.putAll(strategyAwardSearchRateTable);
    }

    /**
     * 获取抽奖策略的奖品ID，也就是抽奖
     * @param strategyId
     * @param rateKey 查找表中的索引（0~9999）
     */
    @Override
    public Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId, rateKey);
    }

    /**
     * 获取抽奖策略的范围值
     * @param strategyId
     * @return
     */
    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + strategyId);
    }




}
