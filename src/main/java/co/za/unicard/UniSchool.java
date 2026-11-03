package co.za.unicard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UniSchool by Unicard: one app for parents, learners, teachers, the principal,
 * the school office and the tuckshop.
 * Run this class in IntelliJ, or use: mvn spring-boot:run
 */
@SpringBootApplication
@EnableScheduling
public class UniSchool {
    public static void main(String[] args) {
        SpringApplication.run(UniSchool.class, args);
    }
}
