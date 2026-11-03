package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Someone signed up and is waiting for the school office to verify them.
 * Parents name their children and learners give their own details: learners already in UniSchool
 * are linked straight away by student number, the rest are created when the office approves.
 */
@Entity
@Table(name = "access_requests")
public class AccessRequest {

    public enum Status { PENDING, APPROVED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role requestedRole;

    /** What they asked for, e.g. "Grade 5 teacher, Highveld Ridge Prep". */
    private String ask;

    /** How they signed up, e.g. "Student numbers matched to the enrolment record". */
    private String how;

    /** Parents and learners: learners already in UniSchool, linked on approval. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "access_request_learners", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "learner_id")
    private Set<Long> learnerIds = new LinkedHashSet<>();

    /** Parents and learners: learners to create on approval because they aren't in UniSchool yet. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "access_request_new_learners", joinColumns = @JoinColumn(name = "request_id"))
    private List<RequestedLearner> newLearners = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    protected AccessRequest() {}

    public AccessRequest(UserAccount account, Role requestedRole, String ask, String how, Set<Long> learnerIds, LocalDateTime createdAt) {
        this.account = account;
        this.requestedRole = requestedRole;
        this.ask = ask;
        this.how = how;
        if (learnerIds != null) this.learnerIds.addAll(learnerIds);
        this.createdAt = createdAt;
    }

    public void decide(Status status, LocalDateTime at) {
        this.status = status;
        this.decidedAt = at;
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public Role getRequestedRole() { return requestedRole; }
    public String getAsk() { return ask; }
    public String getHow() { return how; }
    public Set<Long> getLearnerIds() { return learnerIds; }
    public List<RequestedLearner> getNewLearners() { return newLearners; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
