package elte.peterpolena.graph;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GraphApplication {

	public static void main(String[] args) {

		SpringApplicationBuilder builder = new SpringApplicationBuilder(GraphApplication.class);
		builder.headless(false).run(args);

		new Window();
	}

}
