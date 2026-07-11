package com.edu.archive.config;


// 【操作日志AOP切面】自动拦截Controller的POST/PUT/DELETE请求，记录操作日志到sys_log表。
import com.edu.archive.common.SecurityUtils;
import com.edu.archive.entity.SysLog;
import com.edu.archive.mapper.SysLogMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

@Aspect
@Component
public class OperationLogAspect {
    private final SysLogMapper sysLogMapper;
    private static final Set<String> METHOD_WHITE_LIST = Set.of("POST", "PUT", "DELETE");

    @Autowired
    public OperationLogAspect(SysLogMapper sysLogMapper) {
        this.sysLogMapper = sysLogMapper;
    }

    @AfterReturning("execution(* com.edu.archive.controller..*(..))")
    public void saveLog(JoinPoint jp) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null || !METHOD_WHITE_LIST.contains(attrs.getRequest().getMethod())) {
            return;
        }
        SysLog log = new SysLog();
        log.setLogType("OPERATION");
        log.setUsername(SecurityUtils.username());
        log.setModuleName(jp.getSignature().getDeclaringType().getSimpleName());
        log.setActionName(jp.getSignature().getName());
        log.setResultStatus("SUCCESS");
        log.setDetailJson("{\"detail\":\"auto-log\"}");
        sysLogMapper.insert(log);
    }
}
