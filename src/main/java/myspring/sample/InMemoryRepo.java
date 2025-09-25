package myspring.sample;

import myspring.core.annotation.Component;

@Component
public class InMemoryRepo implements Repo{
    public String find(String data) {
        return data;
    }
}
