package lance.gsr_take_home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class GsrTakeHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(GsrTakeHomeApplication.class, args);
	}

}
