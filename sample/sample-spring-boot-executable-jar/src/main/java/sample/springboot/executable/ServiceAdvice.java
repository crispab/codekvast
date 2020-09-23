package sample.springboot.executable;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class ServiceAdvice {

  @PostConstruct
  public void logAspectLoaded() {
    log.info("Loaded aspect");
  }


  @Around("execution(* sample.springboot.executable.*Service.*(..))")
  public Object auditIngest(ProceedingJoinPoint pjp) throws Throwable {
    log.info("Before {}", pjp);
    try {
      return pjp.proceed();
    } finally {
      log.info("After {}", pjp);
    }
  }
}
