package co.za.unicard.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** The school's stationery list for one grade at one campus. Parents see it once published. */
@Entity
@Table(name = "supply_lists", uniqueConstraints = @UniqueConstraint(columnNames = {"campus", "grade"}))
public class SupplyList {

    public enum Status { DRAFT, PUBLISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    @Column(nullable = false)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<SupplyListItem> items = new ArrayList<>();

    protected SupplyList() {}

    public SupplyList(Campus campus, String grade) {
        this.campus = campus;
        this.grade = grade;
    }

    public void replaceItems(List<SupplyListItem> newItems) {
        items.clear();
        for (int i = 0; i < newItems.size(); i++) {
            newItems.get(i).attach(this, i);
            items.add(newItems.get(i));
        }
    }

    public Long getId() { return id; }
    public Campus getCampus() { return campus; }
    public String getGrade() { return grade; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<SupplyListItem> getItems() { return items; }
}
