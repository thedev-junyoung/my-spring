package myspring.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 메타 애노테이션: 설정 클래스 자체도 컴포넌트 스캔 대상
public @interface Configuration { }
