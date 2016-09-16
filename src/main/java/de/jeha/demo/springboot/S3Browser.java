package de.jeha.demo.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author jenshadlich@googlemail.com
 */
@SpringBootApplication
public class S3Browser {

    public static void main(String[] args) throws Exception {
        new SpringApplication(S3Browser.class).run(args);
    }

}
