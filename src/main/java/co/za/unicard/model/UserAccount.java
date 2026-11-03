package co.za.unicard.model;

import co.za.unicard.util.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A person who signs in with their Unicard ID. Parents also hold a Unicard wallet. */
@Entity
@Table(name = "accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    /** How others see this person in notices and groups, e.g. "Mrs Naidoo". */
    private String displayName;

    /** Short description shown at sign-in, e.g. "Teacher and parent". */
    private String summary;

    private boolean active = true;

    /**
     * Optimistic locking: if two requests change this row at the same moment (e.g. a parent
     * moving money while the till charges the card), the second one fails instead of
     * overwriting the first.
     */
    @Version
    private Long version;

    /** Parent's Unicard wallet. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal walletBalance = Money.ZERO;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<RoleAssignment> roles = new ArrayList<>();

    protected UserAccount() {}

    public UserAccount(String username, String passwordHash, String fullName, String displayName, String summary) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.displayName = displayName;
        this.summary = summary;
    }

    public RoleAssignment addRole(RoleAssignment r) {
        r.attach(this, roles.size());
        roles.add(r);
        return r;
    }

    public Optional<RoleAssignment> role(Role role) {
        return roles.stream().filter(r -> r.getRole() == role).findFirst();
    }

    public Optional<RoleAssignment> firstVerifiedRole() {
        return roles.stream().filter(RoleAssignment::isVerified).findFirst();
    }

    public void credit(BigDecimal amount) { walletBalance = walletBalance.add(amount); }
    public void debit(BigDecimal amount) { walletBalance = walletBalance.subtract(amount); }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getDisplayName() { return displayName != null ? displayName : fullName; }
    public String getFirstName() { return fullName.split(" ")[0]; }
    public String getSummary() { return summary; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public List<RoleAssignment> getRoles() { return roles; }
}
