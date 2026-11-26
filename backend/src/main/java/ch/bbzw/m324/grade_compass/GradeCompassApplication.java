package ch.bbzw.m324.grade_compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Einstiegspunkt: startet Spring Boot mit dem eingebetteten Tomcat (Port 8080)
public class GradeCompassApplication {

	public static void main(String[] args) {
		SpringApplication.run(GradeCompassApplication.class, args);
	}

}
