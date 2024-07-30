package cn.gentlewind.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/**
 * @ClassName 奖品表数据持久化对象
 */
@Data
public class AwardPO {

    /**
     * 自增ID
     */
    private  Long id;
    /**
     * 抽奖奖品ID - 内部流转使用
     */
    private  Long awardId;
    /**
     * 奖品对接标识 - 每一个都是一个对应的发奖策略
     */
    private  String awardKey;
    /**
     * 奖品配置信息
     */
    private  String awardConfig;
    /**
     * 奖品内容描述
     */
    private String awardDesc;
    /**
     * 创建时间
     */
    private Date create_time;
    /**
     * 更新时间
     */
    private  Date update_time;
}
