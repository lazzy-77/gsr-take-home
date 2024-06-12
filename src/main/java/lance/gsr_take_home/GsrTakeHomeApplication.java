package lance.gsr_take_home;

import lance.gsr_take_home.kafka.MessageConsumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GsrTakeHomeApplication {

	public static void main(String[] args) {
		var consumer = new MessageConsumer("kraken");
		new Thread(consumer::consumeMessages).start();

		SpringApplication.run(GsrTakeHomeApplication.class, args);
	}

}
