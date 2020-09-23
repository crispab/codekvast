package sample.app;

import javax.annotation.PostConstruct;
import lombok.extern.java.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Log
public class SampleAspect {

  @PostConstruct
  public void logAspectLoaded() {
    logger.info("Aspect loaded");
  }

  @Around("execution(* sample.app.SampleService*.*(..))")
  public Object aroundSampleService(ProceedingJoinPoint pjp) throws Throwable {
    logger.info("Before " + pjp);
    try {
      return pjp.proceed();
    } finally {
      logger.info("After " + pjp);
    }
  }
}
