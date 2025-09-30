package myspring.config;

import myspring.core.annotation.Bean;
import myspring.core.annotation.Configuration;
import myspring.core.annotation.ScopeType;
import myspring.sample.Repo;
import myspring.sample.InMemoryRepo;
import myspring.sample.Service;

@Configuration
public class AppConfig {

    // 구현체를 코드로 고정하고 싶을 때
    @Bean
    Repo repo() {
        System.out.println("AppConfig.repo() called");
        return new InMemoryRepo();
    }

    // 파라미터 DI: Repo 빈을 주입받아 조립
    @Bean
    Service service(Repo repo) {
        System.out.println("AppConfig.service() called");
        return new Service(repo);
    }

    @Bean(scope = ScopeType.PROTOTYPE)
    String uuid() {
        System.out.println("AppConfig.uuid() called");
        return java.util.UUID.randomUUID().toString();
    }
}
