package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.ParentGroup;
import co.za.unicard.model.SportOffer;
import co.za.unicard.model.UniformItem;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Setting a school up from an empty database: enrolling learners, loading fees, the uniform
 * shop, sport kit, parent groups, the tuckshop menu and learner sign-ins. Everything the
 * demo seeder creates in dev, staff and parents create here in staging and production.
 */
public final class SetupDtos {
    private SetupDtos() {}

    // ---- learners (school office) ----
    public record EnrolLearnerRequest(@NotBlank @Size(max = 20) String studentNumber,
                                      @NotBlank @Size(max = 60) String firstName,
                                      @NotBlank @Size(max = 60) String lastName,
                                      @NotBlank String grade,
                                      @NotNull Campus campus,
                                      @Size(max = 40) String house,
                                      Set<String> sports) {}

    public record UpdateLearnerRequest(@NotBlank @Size(max = 60) String firstName,
                                       @NotBlank @Size(max = 60) String lastName,
                                       @NotBlank String grade,
                                       @NotNull Campus campus,
                                       @Size(max = 40) String house,
                                       Set<String> sports) {}

    /** A learner on the enrolment record, as the office sees them. */
    public record EnrolledLearner(Long id, String studentNumber, String firstName, String lastName, String grade,
                                  Campus campus, String house, Set<String> sports, int guardians, boolean hasSignIn) {}

    // ---- fees (school office) ----
    public record LoadFeesRequest(@NotNull Campus campus,
                                  @NotBlank String grade,
                                  @NotNull @DecimalMin("1.00") @DecimalMax("1000000.00") BigDecimal annualFee) {}

    /** One grade's fees for next year: how many learners have a fee account, at what fee. */
    public record GradeFees(Campus campus, String grade, int learners, int loaded, BigDecimal annualFee) {}

    public record FeeSchedule(int year, List<GradeFees> grades) {}

    // ---- stationery lists (school office) ----
    /** A grade's list with its items, so the office can edit it rather than start again. */
    public record SupplyListDetail(Campus campus, String grade, String status, List<SchoolDtos.ListItemRequest> items) {}

    // ---- uniform shop and sport (school office) ----
    public record UniformItemRequest(@NotNull Campus campus,
                                     @NotBlank @Size(max = 80) String name,
                                     @NotNull @DecimalMin("0.01") BigDecimal price,
                                     @NotNull UniformItem.Kind kind,
                                     @NotBlank String sizeType,
                                     @Size(max = 60) String supplier) {}

    public record UniformItemView(Long id, Campus campus, String name, BigDecimal price, UniformItem.Kind kind,
                                  String sizeType, String supplier) {}

    public record PriceRequest(@NotNull @DecimalMin("0.01") BigDecimal price) {}

    public record StockRequest(@NotBlank @Size(max = 80) String itemName,
                               @NotBlank @Size(max = 20) String size,
                               @Min(0) @Max(100000) int quantity) {}

    public record QuantityRequest(@Min(0) @Max(100000) int quantity) {}

    public record SportOfferRequest(@NotBlank @Size(max = 40) String sport,
                                    @NotNull SportOffer.Type type,
                                    @NotBlank @Size(max = 80) String name,
                                    @NotNull @DecimalMin("0.01") BigDecimal price) {}

    public record SportOfferView(Long id, String sport, SportOffer.Type type, String name, BigDecimal price) {}

    // ---- parent groups (school office) ----
    public record NewGroupRequest(@NotBlank @Size(max = 80) String name,
                                  @NotNull Campus campus,
                                  String grade,
                                  String team,
                                  @Size(max = 80) String moderatorLabel,
                                  @NotNull ParentGroup.Mode mode) {}

    // ---- teacher approval (school office) ----
    /** What a newly approved teacher teaches. Needed for teachers (checked by the service); ignored for other roles. */
    public record TeacherAssignment(Campus campus, String grade, @Size(max = 10) String className) {}

    // ---- tuckshop menu ----
    public record MenuItemRequest(@NotBlank @Size(max = 60) String name,
                                  @NotNull @DecimalMin("0.01") @DecimalMax("1000.00") BigDecimal price,
                                  @NotBlank @Size(max = 40) String category) {}

    public record MenuItemUpdate(@DecimalMin("0.01") @DecimalMax("1000.00") BigDecimal price,
                                 @Size(max = 40) String category,
                                 Boolean available) {}

    public record MenuItemAdminView(Long id, String name, BigDecimal price, String category, boolean available) {}

    // ---- learner sign-in (parent) ----
    public record LearnerSignInRequest(@NotBlank @Size(min = 3, max = 40) String username,
                                       @NotBlank @Size(min = 8, max = 100) String password) {}

    /** The learner's own sign-in, if their parent has made one. */
    public record LearnerSignIn(String username) {}
}
