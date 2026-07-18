package dev.nblucas.facialreconbackend;

import org.springframework.boot.SpringApplication;

public class TestFacialreconbackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(FacialreconbackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
