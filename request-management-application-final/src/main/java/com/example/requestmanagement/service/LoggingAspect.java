package com.example.requestmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.requestmanagement.service..*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.nanoTime();

        log.info("STARTED : {}", methodName);

        try {

            Object result = joinPoint.proceed();

            long executionTime = elapsedMillis(startTime);

            log.info("ENDED : {} : execution time={} ms", methodName, executionTime);

            return result;

        } catch (Exception ex) {

            long executionTime = elapsedMillis(startTime);

            log.error("ERROR : {} : execution time={} ms : {}", methodName, executionTime, ex.getMessage(), ex);

            throw ex;
        }
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}