package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record SwitchRoleRequest(@NotNull Role role) {}

    /**
     * Sign-up for any role. Nobody can sign in until the school office approves the request.
     * Parents describe their children and learners describe themselves; learners who aren't in
     * UniSchool yet are created when the office approves.
     */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 40) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String fullName,
            @NotNull Role role,
            String ask,
            /** Parents: their children. */
            List<@Valid LearnerDetails> children,
            /** Learners: themselves. */
            @Valid LearnerDetails learner,
            /** Teachers: what they teach. Used when sign-ups are approved automatically (local testing); otherwise the office sets it when approving. */
            SetupDtos.TeacherAssignment teaching) {}

    /** A learner as a parent or learner describes them. The student number is optional. */
    public record LearnerDetails(
            @Size(max = 20) String studentNumber,
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank String grade,
            @NotNull Campus campus) {}

    public record RoleView(Long id, Role role, String label, Campus campus, String grade, String className,
                           Long learnerId, boolean verified) {
        public static RoleView of(RoleAssignment r) {
            return new RoleView(r.getId(), r.getRole(), r.getLabel(), r.getCampus(), r.getGrade(), r.getClassName(),
                    r.getLearner() == null ? null : r.getLearner().getId(), r.isVerified());
        }
    }

    public record MeResponse(Long id, String username, String fullName, String summary, RoleView activeRole, List<RoleView> roles) {
        public static MeResponse of(UserAccount a, RoleAssignment active) {
            return new MeResponse(a.getId(), a.getUsername(), a.getFullName(), a.getSummary(),
                    RoleView.of(active), a.getRoles().stream().map(RoleView::of).toList());
        }
    }

    public record TokenResponse(String token, Instant expiresAt, MeResponse me) {}

    public record RegisterResponse(Long requestId, String message) {}
}
