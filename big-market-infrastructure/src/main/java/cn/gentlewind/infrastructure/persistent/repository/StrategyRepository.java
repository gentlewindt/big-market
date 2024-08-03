package cn.gentlewind.infrastructure.persistent.repository;

import cn.gentlewind.domain.strategy.model.entity.StrategyAwardEntity;
import cn.gentlewind.domain.strategy.model.entity.StrategyEntity;
import cn.gentlewind.domain.strategy.model.entity.StrategyRuleEntity;
import cn.gentlewind.domain.strategy.repository.IStrategyRepository;
import cn.gentlewind.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.gentlewind.infrastructure.persistent.dao.IStrategyDao;
import cn.gentlewind.infrastructure.persistent.dao.IStrategyRuleDao;
import cn.gentlewind.infrastructure.persistent.po.StrategyAwardPO;
import cn.gentlewind.infrastructure.persistent.po.StrategyPO;
import cn.gentlewind.infrastructure.persistent.po.StrategyRulePO;
import cn.gentlewind.infrastructure.persistent.redis.IRedisService;
import cn.gentlewind.types.common.Constants;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IStrategyDao strategyDao;
    @Resource
    private IStrategyRuleDao strategyRuleDao;
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
     * @param key
     * @param rateRange
     * @param strategyAwardSearchRateTable
     */
    @Override
    public void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> strategyAwardSearchRateTable) {
        // 1. 存储抽奖策略范围值，如10000
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key, rateRange);
        // 2. 存储概率查找表
        // 从获取或创建一个map
        Map<Integer,Integer> cacheRateTable = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key);
        // 将value存入map
        cacheRateTable.putAll(strategyAwardSearchRateTable);
    }

    /**
     * 获取抽奖策略的奖品ID，也就是抽奖
     * @param key
     * @param rateKey 查找表中的索引（0~9999）
     */
    @Override
    public Integer getStrategyAwardAssemble(String key, Integer rateKey) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key, rateKey);
    }

    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + strategyId);
    }

    @Override
    public int getRateRange(String key) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key);
    }

    /**
     * 获取抽奖策略的范围值
     *
     * 比如抽奖次数多后解锁更高价值奖品
     *
     * @param strategyId
     * @return
     */
    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        // 优先从缓存获取抽奖策略
        // 构建一个缓存键
        String cacheKey = Constants.RedisKey.STRATEGY_KEY + strategyId;
        // 获取策略实体
        StrategyEntity strategyEntity = redisService.getValue(cacheKey);
        if (null != strategyEntity) return strategyEntity;
        StrategyPO strategy = strategyDao.queryStrategyByStrategyId(strategyId);
        strategyEntity = StrategyEntity.builder()
                .strategyId(strategy.getStrategyId())
                .strategyDesc(strategy.getStrategyDesc())
                .ruleModels(strategy.getRuleModels())
                .build();
        redisService.setValue(cacheKey, strategyEntity);
        return strategyEntity;
    }

    /**
     * 根据策略id和规则模型查询策略规则
     *
     * @param strategyId
     * @param ruleModel
     * @return 返回策略规则实体
     */
    @Override
    public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
        StrategyRulePO strategyRuleReq = new StrategyRulePO();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setRuleModel(ruleModel);
        StrategyRulePO strategyRuleRes = strategyRuleDao.queryStrategyRule(strategyRuleReq);
        return StrategyRuleEntity.builder()
                .strategyId(strategyRuleRes.getStrategyId())
                .awardId(strategyRuleRes.getAwardId())
                .ruleType(strategyRuleRes.getRuleType())
                .ruleModel(strategyRuleRes.getRuleModel())
                .ruleValue(strategyRuleRes.getRuleValue())
                .ruleDesc(strategyRuleRes.getRuleDesc())
                .build();
    }

    /**
     * 根据策略id和规则模型查询策略规则值
     * @param strategyId
     * @param awardId
     * @param ruleModel
     * @return
     */
    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRulePO strategyRule = new StrategyRulePO();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setAwardId(awardId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);
    }
}
