package myspring.core;

import myspring.core.annotation.*;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class ApplicationContext implements AutoCloseable {

    // 싱글톤 캐시: SINGLETON 스코프의 인스턴스만 저장
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    // 빈 정의 메타: 컴포넌트 클래스 → 스코프(SINGLETON/PROTOTYPE)
    private final Map<Class<?>, ScopeType> beanDefinitions = new HashMap<>();


    // @Bean 메서드 메타: 반환 타입 → (설정 인스턴스, 메서드, 스코프)
    private static record BeanMethodMeta(Object configInstance, Method method, ScopeType scope) {}
    private final Map<Class<?>, BeanMethodMeta> beanMethodsByType = new HashMap<>();

    private final Set<Class<?>> components;
    private final Set<Class<?>> creating = new HashSet<>();

    // 라이프사이클 위임자
    private final LifecycleProcessor lifecycle = new LifecycleProcessor();


    private ApplicationContext(String basePackage) {
        // 1) @Component 스캔
        Reflections reflections = new Reflections(basePackage);
        this.components = reflections.getTypesAnnotatedWith(Component.class);

        // 2) @Configuration 클래스 처리: 인스턴스 만들고 @Bean 메서드 등록
        Set<Class<?>> configs = reflections.getTypesAnnotatedWith(Configuration.class);
        for (Class<?> cfgClass : configs) {
            Object cfg = getOrCreateAccordingToComponentRules(cfgClass); // DI 지원
            for (Method m : cfgClass.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Bean.class)) {
                    Bean beanAnno = m.getAnnotation(Bean.class);
                    ScopeType scope = beanAnno.scope();
                    Class<?> returnType = m.getReturnType();
                    if (beanMethodsByType.containsKey(returnType)) {
                        throw new IllegalArgumentException("Duplicate @Bean return type: " + returnType);
                    }
                    m.setAccessible(true);
                    beanMethodsByType.put(returnType, new BeanMethodMeta(cfg, m, scope));
                    // 타입 기준 조회가 가능하도록 정의에도 등록
                    beanDefinitions.put(returnType, scope);
                }
            }
        }
        // 싱글톤만 eager 생성 + PostConstruct 호출은 lifecycle이 담당
        for (Class<?> c : components) {
            ScopeType scope = ScopeType.SINGLETON;
            if (c.isAnnotationPresent(Scope.class)) {
                scope = c.getAnnotation(Scope.class).value();
            }
            beanDefinitions.put(c, scope);
        }
    }

    public static ApplicationContext of(String basePackage) {
        return new ApplicationContext(basePackage);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        // 1) 정확 매칭: 정의에 등록된 클래스면 스코프에 맞게 반환
        if (beanDefinitions.containsKey(type)) {
            return (T) getAccordingToScope(type);
        }

        // 2) 다형성 매칭: 컴포넌트들 중 assignable 후보 수집
        List<Class<?>> candidates = components.stream()
                .filter(type::isAssignableFrom)
                .toList();

        if (candidates.size() == 1) {
            return (T) getAccordingToScope(candidates.get(0));
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No bean of type: " + type.getName());
        }
        throw new IllegalArgumentException("No unique bean of type: " + type.getName() + ", candidates=" + candidates);
    }

    // 스코프에 따른 반환 전략
    private Object getAccordingToScope(Class<?> clazz) {
        ScopeType scope = beanDefinitions.getOrDefault(clazz, ScopeType.SINGLETON);
        if (scope == ScopeType.SINGLETON) {
            return getOrCreateSingleton(clazz); // SINGLETON은 캐시 사용
        } else if (scope == ScopeType.PROTOTYPE) {
            return createNewInstanceGraph(clazz); // PROTOTYPE은 항상 새로 생성
        }
        throw new IllegalStateException("Unsupported scope: " + scope);
    }

    // ===== 생성 로직 =====

    // 싱글톤 전용: 캐시에 있으면 꺼내고, 없으면 만들어서 캐시에 저장
    private Object getOrCreateSingleton(Class<?> clazz) {
        Object existing = singletons.get(clazz);
        if (existing != null) return existing;
        Object created = createNewInstanceGraph(clazz);
        singletons.put(clazz, created);
        return created;
    }



    // 생성 그래프: 컴포넌트 클래스 vs @Bean 메서드 반환 타입을 구분 처리
    private Object createNewInstanceGraph(Class<?> clazz) {
        if (creating.contains(clazz))
            throw new IllegalStateException("Circular dependency detected at: " + clazz.getName());
        creating.add(clazz);
        try {
            // 1) @Bean 메서드 반환 타입이면 메서드 호출로 생성
            BeanMethodMeta meta = beanMethodsByType.get(clazz);
            if (meta != null) {
                Object[] args = Arrays.stream(meta.method.getParameterTypes())
                        .map(this::resolveDependency)
                        .toArray();
                try {
                    Object instance = meta.method.invoke(meta.configInstance, args);
                    if (instance == null) {
                        throw new IllegalStateException("@Bean method returned null: " +
                                meta.configInstance.getClass().getName() + "#" + meta.method.getName());
                    }
                    lifecycle.invokePostConstruct(instance);
                    return instance;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke @Bean: " +
                            meta.configInstance.getClass().getName() + "#" + meta.method.getName(), e);
                }
            }

            // 2) 일반 @Component 클래스면 생성자 주입
            Constructor<?> ctor = selectConstructor(clazz);
            Object[] args = Arrays.stream(ctor.getParameterTypes())
                    .map(this::resolveDependency)
                    .toArray();
            ctor.setAccessible(true);
            Object instance = newInstance(ctor, args);
            lifecycle.invokePostConstruct(instance);
            return instance;
        } finally {
            creating.remove(clazz);
        }
    }

    // @Inject 1개 우선, 없으면 기본 생성자
    private Constructor<?> selectConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        List<Constructor<?>> injects = Arrays.stream(ctors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        if (injects.size() > 1) {
            throw new IllegalStateException("@Inject constructor must be single: " + clazz.getName());
        }
        if (injects.size() == 1) return injects.get(0);
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor: " + clazz.getName(), e);
        }
    }

    private Object resolveDependency(Class<?> depType) {
        // 정의된 모든 타입(컴포넌트 + @Bean 반환 타입)에서 탐색
        Optional<Class<?>> target = beanDefinitions.keySet().stream()
                .filter(depType::isAssignableFrom)
                .findFirst();
        if (target.isEmpty()) {
            throw new IllegalStateException("Unsatisfied dependency: " + depType.getName());
        }
        Class<?> targetClass = target.get();
        ScopeType scope = beanDefinitions.getOrDefault(targetClass, ScopeType.SINGLETON);
        return (scope == ScopeType.SINGLETON)
                ? getOrCreateSingleton(targetClass)    // 싱글톤은 캐시
                : createNewInstanceGraph(targetClass); // 프로토타입은 새로 생성
    }

    // 설정 클래스 자체를 컴포넌트 규칙으로 생성
    private Object getOrCreateAccordingToComponentRules(Class<?> clazz) {
        ScopeType scope = beanDefinitions.getOrDefault(clazz, ScopeType.SINGLETON);
        return (scope == ScopeType.SINGLETON) ? getOrCreateSingleton(clazz)
                : createNewInstanceGraph(clazz);
    }

    private static Object newInstance(Constructor<?> ctor, Object[] args) {
        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + ctor.getDeclaringClass().getName(), e);
        }
    }
    @Override
    public void close() {
        lifecycle.invokePreDestroyAll(singletons.values());
    }
}
