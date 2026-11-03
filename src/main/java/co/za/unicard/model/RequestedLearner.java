package co.za.unicard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A learner named in an access request who isn't in UniSchool yet. The school office creates
 * them when it approves the request.
 */
@Embeddable
public class RequestedLearner {

    private String studentNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    protected RequestedLearner() {}

    public RequestedLearner(String studentNumber, String firstName, String lastName, String grade, Campus campus) {
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.grade = grade;
        this.campus = campus;
    }

    public String getStudentNumber() { return studentNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGrade() { return grade; }
    public Campus getCampus() { return campus; }
}
