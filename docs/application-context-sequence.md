## ApplicationContext Full Sequence Diagram
- 스코프 추가, 지연 초기화, 다형성 조회 등을 포함한 시퀀스 다이어그램 
### 포인트 요약
- 싱글톤: 초기화 때(또는 최초 요청 시) 한 번 생성 → singletons 캐시에 저장 후 재사용.
- 프로토타입: 요청/주입 시마다 새로 생성, 캐시에 저장하지 않음.
- 의존성 주입도 스코프 규칙을 그대로 따름(싱글톤 주입=캐시, 프로토타입 주입=새 생성).

```mermaid
sequenceDiagram
    participant User as User Code
    participant AppCtx as ApplicationContext
    participant Ref as Reflections
    participant Def as beanDefinitions(클래스→스코프)
    participant Single as singletons(Map)
    participant Comp as @Component Classes
    participant Ctor as Constructor Reflection

    Note over AppCtx: 컨텍스트 초기화 시점
    User->>AppCtx: ApplicationContext.of("myspring.core")
    AppCtx->>Ref: scan for @Component classes
    Ref-->>AppCtx: return [C1, C2, ...]
    AppCtx->>Def: 등록: Ck -> ScopeType (기본 SINGLETON)
    loop Eager init for SINGLETON only
        AppCtx->>AppCtx: getOrCreateSingleton(Ck)
        alt 캐시에 있음
            AppCtx->>Single: get(Ck)
            Single-->>AppCtx: instance
        else 캐시에 없음
            AppCtx->>Ctor: selectConstructor(Ck)
            AppCtx->>AppCtx: resolveDependency(params)
            AppCtx->>Ctor: newInstance(args)
            AppCtx->>Single: put(Ck, instance)
        end
    end

    Note over AppCtx: 런타임 조회 시점
    User->>AppCtx: getBean(T)
    alt 정확 매칭: Def.contains(T)
        AppCtx->>Def: scope = Def[T]
        alt scope == SINGLETON
            AppCtx->>Single: getOrCreateSingleton(T)
            Single-->>AppCtx: instance
        else scope == PROTOTYPE
            AppCtx->>AppCtx: createNewInstanceGraph(T)
            AppCtx->>Ctor: selectConstructor(T)
            AppCtx->>AppCtx: resolveDependency(params)
            AppCtx->>Ctor: newInstance(args)
            AppCtx-->>User: prototype instance (no cache)
        end
    else 다형성 매칭 (인터페이스/상위타입)
        AppCtx->>Comp: filter assignable to T
        Comp-->>AppCtx: candidates
        alt 후보 1개
            AppCtx->>Def: scope of candidate
            opt SINGLETON
                AppCtx->>Single: getOrCreateSingleton(candidate)
                Single-->>AppCtx: instance
            end
            opt PROTOTYPE
                AppCtx->>AppCtx: createNewInstanceGraph(candidate)
                AppCtx-->>User: prototype instance
            end
        else 0 or N>1
            AppCtx-->>User: throw IllegalArgumentException
        end
    end
    AppCtx-->>User: return bean

    Note over AppCtx: resolveDependency(depType)
    AppCtx->>Comp: find component assignable to depType
    Comp-->>AppCtx: targetClass
    AppCtx->>Def: scope = Def[targetClass]
    alt dep SINGLETON
        AppCtx->>Single: getOrCreateSingleton(targetClass)
        Single-->>AppCtx: dep instance
    else dep PROTOTYPE
        AppCtx->>AppCtx: createNewInstanceGraph(targetClass)
        AppCtx-->>AppCtx: new dep instance
    end

```

### ApplicationContext Sequence Diagram
```mermaid
sequenceDiagram
participant User as User Code
participant AppCtx as ApplicationContext
participant Ref as Reflections
participant Class as @Component Classes
participant Beans as beans(Map<Class, Object>)

    User->>AppCtx: ApplicationContext.of("myspring.core")
    AppCtx->>Ref: scan for @Component classes
    Ref-->>AppCtx: return [HelloService, ...]
    loop For each component
        AppCtx->>Class: selectConstructor()
        AppCtx->>AppCtx: resolveDependency()
        AppCtx->>Class: newInstance(Constructor, args)
        AppCtx->>Beans: put(Class, instance)
    end
    User->>AppCtx: getBean(HelloService.class)
    AppCtx->>Beans: lookup HelloService
    Beans-->>AppCtx: return instance
    AppCtx-->>User: return HelloService bean

```