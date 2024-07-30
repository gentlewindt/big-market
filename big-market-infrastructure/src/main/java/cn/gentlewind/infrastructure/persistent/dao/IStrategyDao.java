package cn.gentlewind.infrastructure.persistent.dao;

import cn.gentlewind.infrastructure.persistent.po.StrategyPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IStrategyDao {

    List<StrategyPO> queryStrategyList();

}
