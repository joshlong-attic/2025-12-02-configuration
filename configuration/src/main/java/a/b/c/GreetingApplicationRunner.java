package a.b.c;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class GreetingApplicationRunner implements ApplicationRunner {

    private final String name;

    public GreetingApplicationRunner(String name) {
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        IO.println("hello, " + name);
    }
}
