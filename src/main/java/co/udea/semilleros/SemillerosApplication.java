package co.udea.semilleros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SemillerosApplication {

    public static void main(String[] args) {
        SpringApplication.run(SemillerosApplication.class, args);
    }
}
