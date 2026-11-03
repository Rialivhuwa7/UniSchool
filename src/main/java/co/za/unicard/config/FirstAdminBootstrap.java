package co.za.unicard.config;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A new school starts with an empty database, so nobody could approve the first access request.
 * When no school office or principal account exists yet, this creates one school office account
 * from UNISCHOOL_ADMIN_USERNAME / UNISCHOOL_ADMIN_PASSWORD. That person then approves staff and
 * parents through the app. Runs after the demo seeder, so in dev the demo office is used instead.
 */
@Component
@Order(2)
public class FirstAdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FirstAdminBootstrap.class);
    static final int MIN_PASSWORD = 12;

    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String fullName;

    public FirstAdminBootstrap(UserAccountRepository accounts, PasswordEncoder encoder,
                               @Value("${unischool.admin.username:}") String username,
                               @Value("${unischool.admin.password:}") String password,
                               @Value("${unischool.admin.full-name:School office}") String fullName) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.username = username.trim();
        this.password = password;
        this.fullName = fullName.trim();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (accounts.hasSchoolAdmin()) return;
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("No school office account exists yet. Set UNISCHOOL_ADMIN_USERNAME and UNISCHOOL_ADMIN_PASSWORD "
                    + "and restart to create the first one.");
            return;
        }
        if (password.length() < MIN_PASSWORD) {
            throw new IllegalStateException("UNISCHOOL_ADMIN_PASSWORD must be at least " + MIN_PASSWORD + " characters.");
        }
        if (accounts.existsByUsername(username)) {
            throw new IllegalStateException("UNISCHOOL_ADMIN_USERNAME '" + username + "' already belongs to an account without office access.");
        }
        UserAccount admin = new UserAccount(username, encoder.encode(password), fullName, fullName, "School office");
        RoleAssignment office = admin.addRole(new RoleAssignment(Role.SCHOOL_OFFICE, "School office, admin access", Campus.BOTH, false));
        office.verify("Created at first start-up");
        accounts.save(admin);
        log.info("Created the first school office account '{}'. Change its password after signing in.", username);
    }
}
