package br.gov.crateus.bcm.devhost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
		"br.gov.crateus.bcm.devhost",
		"br.gov.crateus.bcm"
})
@EntityScan(basePackages = "br.gov.crateus.bcm")
@EnableJpaRepositories(basePackages = "br.gov.crateus.bcm")
public class BcmDevHostApplication {

	public static void main(String[] args) {
		SpringApplication.run(BcmDevHostApplication.class, args);
	}
}
