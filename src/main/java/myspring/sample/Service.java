package myspring.sample;

import myspring.core.annotation.Component;

@Component
public class Service {

    private final Repo repo;
    public Service(Repo repo) {
        this.repo = repo;
    }
    public String call(String str) {
        System.out.println("Service.call() str = " + str);
        return str + repo.find("data");
    }
}
