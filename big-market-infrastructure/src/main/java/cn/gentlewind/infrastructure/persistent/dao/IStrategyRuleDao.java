package cn.gentlewind.infrastructure.persistent.dao;

import cn.gentlewind.infrastructure.persistent.po.StrategyRulePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyRuleDao {
    List<StrategyRulePO> queryStrategyRuleList();
}
