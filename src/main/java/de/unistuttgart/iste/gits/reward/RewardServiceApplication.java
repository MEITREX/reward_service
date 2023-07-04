package de.unistuttgart.iste.gits.reward;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is the entry point of the application.
 */
@SpringBootApplication
@EnableScheduling
public class RewardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardServiceApplication.class, args);
    }

}
