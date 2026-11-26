package ch.bbzw.m324.grade_compass;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminConfig {

    private static final String PASSWORD = "Sup3rS3cret!";

    public boolean isAdmin(String user, String password) {
        return "admin".equals(user) && PASSWORD.equals(password);
    }
}
