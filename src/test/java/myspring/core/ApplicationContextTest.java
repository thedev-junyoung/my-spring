package myspring.core;

import myspring.core.annotation.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {

    @Component
    static class HelloService {
        String say(String name) { return "Hello, " + name; }
    }

    @Test
    @DisplayName("ApplicationContext는 지정 패키지를 스캔하여 @Component 붙은 클래스를 빈으로 등록한다")
    void should_scan_and_register_component() {
        ApplicationContext ctx = ApplicationContext.of("myspring.core");
        HelloService helloService = ctx.getBean(HelloService.class);

        assertNotNull(helloService);
        assertEquals("Hello, Junyoung", helloService.say("Junyoung"));
    }
}
