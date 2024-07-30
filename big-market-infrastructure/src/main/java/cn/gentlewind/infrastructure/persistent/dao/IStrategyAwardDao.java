package cn.gentlewind.infrastructure.persistent.dao;

import cn.gentlewind.infrastructure.persistent.po.StrategyAwardPO;

import java.util.List;

public interface IStrategyAwardDao {
    List<StrategyAwardPO> queryStrategyAwardList();
}
