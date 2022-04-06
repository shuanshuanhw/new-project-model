package com.example.newprojectmodel.aspectj;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.example.newprojectmodel.annotation.Log;
import com.example.newprojectmodel.entity.SysOperLog;
import com.example.newprojectmodel.entity.SysUser;
import com.example.newprojectmodel.enums.BusinessStatus;
import com.example.newprojectmodel.manager.AsyncManager;
import com.example.newprojectmodel.utils.ServletUtils;
import com.example.newprojectmodel.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

/**
 * 操作日志记录处理
 * 
 * @author ruoyi
 */
@Aspect
@Component
public class LogAspect
{
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 排除敏感属性字段 */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    // 配置织入点
    // @annotation：当执行的方法上拥有指定的注解时生效。
    @Pointcut("@annotation(com.example.newprojectmodel.annotation.Log)")
    public void logPointCut()
    {
    }

    /**
     * 处理完请求后执行
     * @param joinPoint 切点
     */
    // controllerLog应该是随意定的，只要和下面的LOG变量对应就行，用来传递方法上的注解注入参数
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")

    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult)
    {
        // joinPoint 应该是用来存储应用此注解的方法的资料
        // jsonResult是方法的返回值
        // controllerLog是注解时用户自己添加的注解参数的值的集合
          handleLog(joinPoint, controllerLog, null, jsonResult);
    //    System.out.println(controllerLog);
     //   System.out.println("已经完成了方法");
    }

    /**
     * 拦截异常操作
     * 
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e)
    {
      //  handleLog(joinPoint, controllerLog, e, null);
        System.out.println("发生异常");
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult)
    {
        try
        {
            // 获取当前的用户
          //  SysUser currentUser = ShiroUtils.getSysUser();
            SysUser currentUser = null;

            // *========数据库日志=========*//
            SysOperLog operLog = new SysOperLog();

            // 1、这里存储此次发起请求在http层面的一些数据
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            // 请求的地址
           // String ip = ShiroUtils.getIp();
            String ip = "199999";
            operLog.setOperIp(ip);
         //   operLog.setOperUrl(ServletUtils.getRequest().getRequestURI());
            operLog.setOperUrl("usrl");

            if (currentUser != null)
            {
                operLog.setOperName(currentUser.getLoginName());
                if (StringUtils.isNotNull(currentUser.getDept())
                        && StringUtils.isNotEmpty(currentUser.getDept().getDeptName()))
                {
                    operLog.setDeptName(currentUser.getDept().getDeptName());
                }
            }
            // 这里存储此次发起请求在http层面的一些数据

            // 针对异常返回
            if (e != null)
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }
            // 针对异常返回

            // 2、设置方法名称 joinPoint
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());

            System.out.println("日志详情："+operLog.toString());

            // 3、处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 保存数据库
            AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
        }
        catch (Exception exp)
        {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     * 
     * @param log 日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, SysOperLog operLog, Object jsonResult) throws Exception
    {
        // 设置action动作
        // ordinal()此方法返回枚举常量的序数。
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData())
        {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog);
        }
        // 是否需要保存response，参数和值
        // 要让这里为真，必须是注解开关为true，并且方法正常执行，非异常。
        if (log.isSaveResponseData() && StringUtils.isNotNull(jsonResult))
        {
            operLog.setJsonResult(StringUtils.substring(JSONObject.toJSONString(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到log中
     * 
     * @param operLog 操作日志
     * @throws Exception 异常
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog) throws Exception
    {
        Map<String, String[]> map = ServletUtils.getRequest().getParameterMap();

        // request.getParameterMap()只能获取Get方式传入的数据。
        if (StringUtils.isNotEmpty(map))
        {
            // JSONObject.toJSONString()默认忽略值为null的属性
            // 将一个map的敏感因素排除掉，再转为json，敏感因素以字符串数组存在，以new PropertyPreFilters().addFilter().addExcludes(EXCLUDE_PROPERTIES)作为过滤器
            String params = JSONObject.toJSONString(map, excludePropertyPreFilter());
            operLog.setOperParam(StringUtils.substring(params, 0, 2000));
        }
        else
        {
            Object args = joinPoint.getArgs();
            if (StringUtils.isNotNull(args))
            {
                // 将参数数组转为字符串形式的json
                String params = argsArrayToString(joinPoint.getArgs());
                operLog.setOperParam(StringUtils.substring(params, 0, 2000));
            }
        }
    }

    /**
     * 忽略敏感属性
     */
    public PropertyPreFilters.MySimplePropertyPreFilter excludePropertyPreFilter()
    {
        return new PropertyPreFilters().addFilter().addExcludes(EXCLUDE_PROPERTIES);
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray)
    {
        String params = "";
        if (paramsArray != null && paramsArray.length > 0)
        {
            for (Object o : paramsArray)
            {
                if (StringUtils.isNotNull(o) && !isFilterObject(o))
                {
                    try
                    {
                        Object jsonObj = JSONObject.toJSONString(o, excludePropertyPreFilter());
                        params += jsonObj.toString() + " ";
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        }
        return params.trim();
    }

    /**
     * 判断是否需要过滤的对象。
     * 
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o)
    {
        Class<?> clazz = o.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) o;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) o;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
