package coo.lxt.island.server.icloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ICloudServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ICloudServerApplication.class, args);
    }
}
