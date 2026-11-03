package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Per-guardian settings for a group: muted, and when they last read it (for unread counts). */
@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "account_id"}))
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ParentGroup group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    private boolean muted;
    private LocalDateTime lastReadAt;

    protected GroupMembership() {}

    public GroupMembership(ParentGroup group, UserAccount account, LocalDateTime lastReadAt) {
        this.group = group;
        this.account = account;
        this.lastReadAt = lastReadAt;
    }

    public Long getId() { return id; }
    public ParentGroup getGroup() { return group; }
    public UserAccount getAccount() { return account; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}
