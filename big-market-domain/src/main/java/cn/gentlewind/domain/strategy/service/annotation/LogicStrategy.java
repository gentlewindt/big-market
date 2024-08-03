package cn.gentlewind.domain.strategy.service.annotation;

import cn.gentlewind.domain.strategy.service.rule.factory.DefaultLogicFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * 自定义注解
 *
 * 用于标记类或接口，并指定了与之关联的逻辑模型
 */
@Target({ElementType.TYPE}) // 指定注解的作用目标为类或接口
@Retention(RetentionPolicy.RUNTIME) // 指定注解的保留策略为运行时(可以在运行时通过反射访问此注解)
public @interface LogicStrategy {

    // 用于指定逻辑模型，返回类型为 DefaultLogicFactory.LogicModel 枚举类型。
    DefaultLogicFactory.LogicModel logicMode();

}
