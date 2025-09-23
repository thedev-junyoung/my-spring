package myspring.core;

import myspring.core.annotation.PostConstruct;
import myspring.core.annotation.PreDestroy;

import java.lang.reflect.Method;
import java.util.Collection;

public class LifecycleProcessor {

    /** 생성/주입 완료 직후 호출 (@PostConstruct 전부 실행) */
    public void invokePostConstruct(Object bean) {
        invokeAnnotated(bean, PostConstruct.class);
    }

    /** 컨텍스트 종료 시 호출 (@PreDestroy 전부 실행) */
    public void invokePreDestroy(Object bean) {
        invokeAnnotated(bean, PreDestroy.class);
    }

    /** 여러 빈에 대해 PreDestroy 실행 (싱글톤 캐시에 대해 사용) */
    public void invokePreDestroyAll(Collection<?> beans) {
        for (Object bean : beans) invokePreDestroy(bean);
    }

    private void invokeAnnotated(Object bean, Class<?> annoType) {
        Class<?> clazz = bean.getClass();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent((Class) annoType)) {
                m.setAccessible(true);
                try {
                    m.invoke(bean);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Lifecycle method failed: " + clazz.getName() + "#" + m.getName(), e);
                }
            }
        }
    }
}
