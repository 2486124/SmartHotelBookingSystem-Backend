package com.cts.shbsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
//@EnableFeignClients(basePackages = "com.cts.shbsm.bookingclient")
public class LoyaltyServiceApplication {

	public static void main(String[] args) {
        SpringApplication.run(LoyaltyServiceApplication.class, args);
	}
}
