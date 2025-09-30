# My-Spring-Framework

Spring Framework의 핵심 원리를 이해하기 위해 직접 구현하는 프로젝트

## 프로젝트 목표

Spring의 핵심 기능들을 Java로 직접 구현하면서 내부 동작 원리를 이해한다

- **IoC Container**: 객체 생성과 의존성 주입의 원리
- **AOP & Transaction**: 횡단 관심사와 트랜잭션 관리
- **Web MVC**: HTTP 요청 처리와 REST API
- **Persistence (JDBC → JPA)**: 데이터 액세스 추상화와 영속성 관리

---

## 구현 목차

### 1. Core Container (IoC & DI)

- `@Component` & `@Inject` - 컴포넌트 스캔과 의존성 주입
- `@Configuration` & `@Bean` - Java 기반 설정
- Bean Lifecycle - 초기화/소멸 콜백
- Bean Scope - Singleton/Prototype
- `@Qualifier` & `@Lazy` - 빈 한정자와 지연 초기화
- BeanPostProcessor - 빈 생성 후처리
- Circular Dependency - 순환 참조 해결
- `@Value` & PropertySource - 설정 값 주입
- ApplicationEventPublisher & `@EventListener`

---

### 2. AOP & Transaction

- Dynamic Proxy - JDK 동적 프록시
- ProxyFactory & Pointcut - 메서드 패턴 매칭
- Advice - `@Around`, `@Before`, `@After`
- `@Transactional` 구현
    - TransactionManager
    - Propagation (REQUIRED)
    - Auto Commit / Rollback

---

### 3. Web MVC

- DispatcherServlet - Front Controller 패턴
- Handler Mapping - `@Controller`, `@RequestMapping`
- HandlerAdapter - 메서드 호출 전략
- Argument Resolver - `@PathVariable`, `@RequestBody`
- Message Converter - JSON 직렬화/역직렬화
- ViewResolver - JSON / JSP 선택
- ExceptionResolver - 예외 처리
- Interceptor - 요청 전후 훅

---

### 4. Persistence Layer (JDBC 기반)

- `DataSource` 관리
- Connection / Statement / ResultSet 직접 처리
- `JdbcTemplate` 흉내내기
    - `query()`, `update()` 메서드 제공
    - RowMapper 패턴
- TransactionManager (JDBC 기반)
- Exception Translation (`SQLException` → `DataAccessException`)

---

### 5. JPA (EntityManager)

- Entity Metadata - `@Entity`, `@Id`, `@Column` 파싱
- EntityManager CRUD
    - `persist()`, `find()`, `remove()`
    - 1차 캐시 (Persistence Context)
- Dirty Checking - 변경 감지와 자동 UPDATE
- Flush & Commit 제어
- Lazy Loading - Proxy 기반 지연 로딩
- 연관관계 매핑 (`@OneToMany`, `@ManyToOne`)

---

### 6. Data JPA

- Repository Interface - `JpaRepository<T, ID>`
- Dynamic Proxy - 구현체 자동 생성
- Query Method - `findBy...` 메서드 파싱
- Pageable / Sort 지원