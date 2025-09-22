package myspring.core.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationsTest {

    @DisplayName("@Component는 TYPE 타깃 + RUNTIME 유지 + value() 디폴트 제공")
    @Test
    void component_meta_shouldBeValid() throws Exception {
        Class<?> anno = Class.forName("myspring.core.annotation.Component");

        // @Target(TYPE)
        Target target = anno.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, target.value());

        // @Retention(RUNTIME)
        Retention retention = anno.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());

        // value() 디폴트 존재
        assertNotNull(anno.getMethod("value"));
        assertEquals("", anno.getMethod("value").getDefaultValue());
    }

    @DisplayName("@Inject는 CONSTRUCTOR 타깃 + RUNTIME 유지")
    @Test
    void inject_meta_shouldBeValid() throws Exception {
        Class<?> anno = Class.forName("myspring.core.annotation.Inject");

        // @Target(CONSTRUCTOR)
        Target target = anno.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.CONSTRUCTOR}, target.value());

        // @Retention(RUNTIME)
        Retention retention = anno.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    // === 기능적 사용 예 (간단 검증; 아직 DI 컨테이너 미구현) ===
    @Component
    static class HelloService {
        String say(String name) { return "Hello, " + name; }
    }

    @Component
    static class HelloController {
        private final HelloService service;

        @Inject
        public HelloController(HelloService service) {
            this.service = service;
        }
    }

    @DisplayName("@Inject는 public/non-public 생성자 모두 허용(리플렉션 주입 가정)")
    @Test
    void inject_constructor_allowsNonPublic() throws Exception {
        class Sample {
            @Inject
            private Sample() {}
        }
        Constructor<?> ctor = Sample.class.getDeclaredConstructors()[0];
        assertTrue(ctor.isAnnotationPresent(Inject.class));
        assertFalse(Modifier.isPublic(ctor.getModifiers())); // private도 허용
    }
}
