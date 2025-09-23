package myspring.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 메서드에 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {
}
