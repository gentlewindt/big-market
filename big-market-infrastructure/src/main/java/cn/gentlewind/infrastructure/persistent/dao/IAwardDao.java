package cn.gentlewind.infrastructure.persistent.dao;

import cn.gentlewind.infrastructure.persistent.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IAwardDao {
    List<AwardPO> queryAwardList();
}
