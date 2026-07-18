package dev.nblucas.facereconbackend;

import org.springframework.boot.SpringApplication;

public class TestFacereconbackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(FacereconbackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
