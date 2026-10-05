package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.demo.topicmanagement.entity.AnnouncementScope;

/** Nội dung và phạm vi của một thông báo mới. */
public class AnnouncementForm {
    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 500, message = "Tiêu đề tối đa 500 ký tự")
    private String title;
    @NotBlank(message = "Vui lòng nhập nội dung")
    private String content;
    @NotNull(message = "Vui lòng chọn phạm vi")
    private AnnouncementScope scope = AnnouncementScope.SCHOOL;
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public AnnouncementScope getScope() { return scope; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setScope(AnnouncementScope scope) { this.scope = scope; }
}
