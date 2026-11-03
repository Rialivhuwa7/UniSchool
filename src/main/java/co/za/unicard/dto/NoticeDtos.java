package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.PublishStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class NoticeDtos {
    private NoticeDtos() {}

    public record ChildAck(Long learnerId, String name, boolean acknowledged) {}

    /** A notice as a parent sees it. */
    public record NoticeView(Long id, String from, String tag, String title, String body, String attachment,
                             String audience, LocalDateTime publishedAt, boolean read, boolean requiresAck,
                             List<ChildAck> children, boolean rsvpEnabled, Boolean myRsvp, Long linkedCollectionId) {}

    /** A short headline for the learner's app. */
    public record Headline(Long id, String from, String tag, String title, LocalDateTime publishedAt) {}

    /** A notice as staff see it, with reach. */
    public record NoticeStats(Long id, String from, String tag, String title, String body, String audience,
                              PublishStatus status, String reviewComment, LocalDateTime createdAt, LocalDateTime publishedAt,
                              long guardians, long reads, int readPercent, boolean requiresAck, long acks, int ackPercent) {}

    public record NewNoticeRequest(@NotBlank @Size(max = 140) String title,
                                   @NotBlank @Size(max = 4000) String body,
                                   @NotNull Campus campus,
                                   Set<String> grades,
                                   Set<String> teams,
                                   String tag,
                                   String attachmentName,
                                   boolean requiresAck,
                                   boolean rsvpEnabled,
                                   Long linkedCollectionId) {}

    public record EditRequest(@NotBlank @Size(max = 140) String title, @NotBlank @Size(max = 4000) String body) {}

    public record RequestChanges(@NotBlank(message = "Tell the teacher what to change.") String comment) {}

    public record RsvpRequest(@NotNull Boolean attending) {}

    public record AckRequest(@NotNull Long learnerId) {}

    public record PostResult(Long id, PublishStatus status, String message) {}
}
