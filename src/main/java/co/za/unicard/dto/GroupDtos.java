package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.ParentGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupDtos {
    private GroupDtos() {}

    public record GroupView(Long id, String name, Campus campus, String school, List<String> forChildren, long members,
                            String moderator, ParentGroup.Mode mode, boolean canPost, boolean muted, long unread) {}

    public record MessageView(Long id, String author, String role, String body, LocalDateTime sentAt, boolean mine,
                              boolean scheduled, LocalDateTime deliverAt) {}

    public record PostMessageRequest(@NotBlank @Size(max = 2000) String body) {}

    public record PostMessageResult(MessageView message, String notice) {}

    public record MuteRequest(boolean muted) {}

    public record ModeRequest(@NotNull ParentGroup.Mode mode) {}

    /** School office view of a group. */
    public record GroupAdminView(Long id, String name, Campus campus, long members, String moderator, ParentGroup.Mode mode,
                                 long reportedMessages) {}

    public record ReportedMessage(Long id, Long groupId, String groupName, String author, String body, int reports,
                                  LocalDateTime sentAt) {}
}
