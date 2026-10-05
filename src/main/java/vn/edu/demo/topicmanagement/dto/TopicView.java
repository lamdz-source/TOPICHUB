package vn.edu.demo.topicmanagement.dto;

import vn.edu.demo.topicmanagement.entity.Topic;

/** Gói 1 đề tài cùng thông tin phụ (tên các GVHD, có được sửa không) để đưa ra trang danh sách. */
public class TopicView {
    private final Topic topic;
    private final String supervisors;   // vd: "Trần Minh An, Lê Thị Bích"
    private final boolean editable;

    public TopicView(Topic topic, String supervisors, boolean editable) {
        this.topic = topic;
        this.supervisors = supervisors;
        this.editable = editable;
    }

    public Topic getTopic() { return topic; }
    public String getSupervisors() { return supervisors; }
    public boolean isEditable() { return editable; }
}
