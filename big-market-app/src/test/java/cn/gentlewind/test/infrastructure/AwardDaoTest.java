package cn.gentlewind.test.infrastructure;

import cn.gentlewind.infrastructure.persistent.dao.IAwardDao;
import cn.gentlewind.infrastructure.persistent.po.AwardPO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * 奖品Dao单测
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardDaoTest {

    @Resource
    private IAwardDao awardDao;

    @Test
    public void test_queryAwardList(){
        List<AwardPO> awards = awardDao.queryAwardList();
        log.info("查询结果：{} ", JSON.toJSONString(awards));

    }

}
