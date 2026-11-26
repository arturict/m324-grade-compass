package ch.bbzw.m324.grade_compass;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Admin-Prüfung. Das Passwort steht nicht im Code, sondern kommt von aussen
 * (Property admin.password, in Produktion aus der Umgebungsvariable ADMIN_PASSWORD).
 */
@Configuration
public class AdminConfig {

    private final String adminSecret;

    public AdminConfig(@Value("${admin.password}") String adminSecret) {
        this.adminSecret = adminSecret;
    }

    public boolean isAdmin(String user, String secret) {
        return "admin".equals(user) && !adminSecret.isEmpty() && adminSecret.equals(secret);
    }
}
