package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;

/** Bản trình bày hội đồng với danh sách thành viên/đề tài đã chuyển thành chuỗi. */
public class CouncilSummaryView {
    private final Long id;
    private final String name;
    private final String periodName;
    private final LocalDateTime defenseDate;
    private final String room;
    private final CouncilStatus status;
    private final List<String> members;
    private final List<String> topics;
    public CouncilSummaryView(Long id, String name, String periodName, LocalDateTime defenseDate, String room,
                             CouncilStatus status, List<String> members, List<String> topics) {
        this.id = id; this.name = name; this.periodName = periodName; this.defenseDate = defenseDate;
        this.room = room; this.status = status; this.members = List.copyOf(members); this.topics = List.copyOf(topics);
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPeriodName() { return periodName; }
    public LocalDateTime getDefenseDate() { return defenseDate; }
    public String getRoom() { return room; }
    public CouncilStatus getStatus() { return status; }
    public List<String> getMembers() { return members; }
    public List<String> getTopics() { return topics; }
}
