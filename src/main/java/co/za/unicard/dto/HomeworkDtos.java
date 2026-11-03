package co.za.unicard.dto;

import co.za.unicard.model.PublishStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class HomeworkDtos {
    private HomeworkDtos() {}

    /** Homework as a learner or parent sees it. */
    public record HomeworkView(Long id, Long learnerId, String learnerName, String subject, String title, String body,
                               LocalDate dueDate, String attachment, String by, boolean done) {}

    /** Homework as staff see it, with how many have finished. */
    public record HomeworkStats(Long id, String grade, String school, String subject, String title, String body,
                                LocalDate dueDate, String attachment, String by, PublishStatus status, String reviewComment,
                                LocalDateTime createdAt, long done, long total, int percentDone) {}

    public record NewHomeworkRequest(@NotBlank String subject,
                                     @NotBlank(message = "Say what learners need to do.") @Size(max = 200) String title,
                                     @Size(max = 2000) String body,
                                     @NotNull LocalDate dueDate,
                                     String attachmentName) {}

    public record DoneRequest(@NotNull Long learnerId, boolean done) {}
}
