package world.jerry.feedhub.collector.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RSS 동기화 이력
 */
@Entity
@Table(name = "sync_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, length = 100)
    private String commandId;

    @Column(name = "rss_info_id")
    private Long rssInfoId;

    @Column(name = "sync_type", nullable = false, length = 20)
    private String syncType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "new_entries", nullable = false)
    private int newEntries;

    @Column(name = "skipped_entries", nullable = false)
    private int skippedEntries;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    public static SyncHistory started(String commandId, Long rssInfoId, String syncType, String requestedBy) {
        SyncHistory history = new SyncHistory();
        history.commandId = commandId;
        history.rssInfoId = rssInfoId;
        history.syncType = syncType;
        history.status = "IN_PROGRESS";
        history.newEntries = 0;
        history.skippedEntries = 0;
        history.startedAt = Instant.now();
        history.requestedBy = requestedBy;
        return history;
    }

    public void complete(int newEntries, int skippedEntries) {
        this.status = "COMPLETED";
        this.newEntries = newEntries;
        this.skippedEntries = skippedEntries;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }
}
