package lance.gsr_take_home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GsrTakeHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(GsrTakeHomeApplication.class, args);

		// Run the application for 5 minutes (300000 milliseconds)
		try {
			Thread.sleep(300000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Application interrupted");
		}

		// Exit the application
		System.exit(0);
	}

}
