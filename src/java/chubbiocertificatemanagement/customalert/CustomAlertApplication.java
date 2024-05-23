package chubbiocertificatemanagement.customalert;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CustomAlertApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(CustomAlertApplication.class, args);
	}

	@Bean
	public CustomAlert getCustomAlertService() {
		return new CustomAlert();
	}

	@Override
	public void run(String... args) throws Exception {
		getCustomAlertService().start();
	}
}
