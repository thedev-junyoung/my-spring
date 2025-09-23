package myspring.core;

import myspring.core.annotation.Component;
import myspring.core.annotation.PostConstruct;
import myspring.core.annotation.PreDestroy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LifecycleProcessor 테스트")
class LifecycleProcessorTest {

    static class Dummy {
        boolean inited, destroyed;

        @PostConstruct void init() { inited = true; }
        @PreDestroy   void bye()  { destroyed = true; }
    }

    @Test
    @DisplayName("@PostConstruct, @PreDestroy 호출")
    void invoke_hooks() {
        LifecycleProcessor lp = new LifecycleProcessor();
        Dummy d = new Dummy();

        lp.invokePostConstruct(d);
        assertTrue(d.inited);

        lp.invokePreDestroy(d);
        assertTrue(d.destroyed);
    }

    @Component
    static class WithPrivate {
        boolean inited;

        @PostConstruct
        private void init() { inited = true; }
    }

    @Test
    @DisplayName("private 메서드도 호출 가능")
    void allows_non_public_methods() {
        LifecycleProcessor lp = new LifecycleProcessor();
        WithPrivate w = new WithPrivate();
        lp.invokePostConstruct(w);
        assertTrue(w.inited);
    }
}
