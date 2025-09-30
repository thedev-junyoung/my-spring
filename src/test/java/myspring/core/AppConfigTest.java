package myspring.core;

import myspring.sample.Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    @DisplayName("@Bean 메서드는 싱글톤 캐시에 등록되고 DI가 적용된다")
    void bean_method_singleton_and_di() {
        ApplicationContext ctx = ApplicationContext.of("myspring");
        Service s1 = ctx.getBean(Service.class);
        Service s2 = ctx.getBean(Service.class);

        assertSame(s1, s2);
        assertEquals("service component:data", s1.call("service component:"));
    }

    @Test
    @DisplayName("@Bean(scope=PROTOTYPE)은 매번 새 인스턴스를 리턴한다")
    void prototype_from_bean_method() {
        ApplicationContext ctx = ApplicationContext.of("myspring");
        String a = ctx.getBean(String.class);   // uuid()
        String b = ctx.getBean(String.class);
        assertNotSame(a, b);
    }
}
