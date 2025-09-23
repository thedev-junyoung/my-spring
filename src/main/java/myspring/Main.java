package myspring;

import myspring.core.ApplicationContext;
import myspring.core.annotation.Component;
import myspring.core.annotation.PostConstruct;
import myspring.core.annotation.PreDestroy;

public class Main {
    public static void main(String[] args) {
        try (ApplicationContext ctx = ApplicationContext.of("myspring")) {
            DemoService demo = ctx.getBean(DemoService.class);
            demo.doWork();
        }
        // 여기서 자동으로 @PreDestroy 메서드 실행됨
    }

    /*
    * 실행결과
    *
        > Task :myspring.Main.main()
        [DemoService] @PostConstruct 실행!
        [DemoService] 작업 중...
        [DemoService] @PreDestroy 실행!
    *
    * */
}


@Component
class DemoService {
    @PostConstruct
    public void init() {
        System.out.println("[DemoService] @PostConstruct 실행!");
    }

    public void doWork() {
        System.out.println("[DemoService] 작업 중...");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("[DemoService] @PreDestroy 실행!");
    }
}