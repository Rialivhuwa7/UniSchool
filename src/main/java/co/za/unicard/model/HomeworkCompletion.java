package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A learner (or their parent) ticked the homework as done. */
@Entity
@Table(name = "homework_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"homework_id", "learner_id"}))
public class HomeworkCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id")
    private Homework homework;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    private LocalDateTime doneAt;

    protected HomeworkCompletion() {}

    public HomeworkCompletion(Homework homework, Learner learner, LocalDateTime doneAt) {
        this.homework = homework;
        this.learner = learner;
        this.doneAt = doneAt;
    }

    public Long getId() { return id; }
    public Homework getHomework() { return homework; }
    public Learner getLearner() { return learner; }
}
