// src/test/java/myspring/core/ScopeTest.java
package myspring.core;

import myspring.core.annotation.Component;
import myspring.core.annotation.Scope;
import myspring.core.annotation.ScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Component
    static class SingletonBean {}

    @Component
    @Scope(ScopeType.PROTOTYPE)
    static class PrototypeBean {}

    @Test
    @DisplayName("기본 스코프는 SINGLETON: 같은 인스턴스를 반환한다")
    void default_is_singleton() {
        ApplicationContext ctx = ApplicationContext.of("myspring.core");
        SingletonBean a = ctx.getBean(SingletonBean.class);
        SingletonBean b = ctx.getBean(SingletonBean.class);
        assertSame(a, b);
    }

    @Test
    @DisplayName("PROTOTYPE 스코프는 매번 새로운 인스턴스를 반환한다")
    void prototype_returns_new_instance_every_time() {
        ApplicationContext ctx = ApplicationContext.of("myspring.core");
        PrototypeBean a = ctx.getBean(PrototypeBean.class);
        PrototypeBean b = ctx.getBean(PrototypeBean.class);
        assertNotSame(a, b);
    }
}
