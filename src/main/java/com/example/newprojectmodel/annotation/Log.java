package com.example.newprojectmodel.annotation;

import com.example.newprojectmodel.enums.BusinessType;
import com.example.newprojectmodel.enums.OperatorType;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 * 
 * @author ruoyi
 */
// PARAMETER应该是能放在参数前，METHOD应该是说这个注解能放在方法上
@Target({ ElementType.PARAMETER, ElementType.METHOD })
// RUNTIME. 注解会被保留在class文件中，同时运行时期间也会被识别。所以可以使用反射机制获取注解信息。比如@Deprecated。
// retention指保留
@Retention(RetentionPolicy.RUNTIME)
// 如果使用@Documented标注了，在生成javadoc的时候就会把注解给显示出来。
@Documented
public @interface Log
{
    /**
     * 模块 
     */
    public String title() default "";

    /**
     * 功能
     */
    public BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别
     */
    public OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求的参数
     */
    public boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的参数
     */
    public boolean isSaveResponseData() default true;
}
