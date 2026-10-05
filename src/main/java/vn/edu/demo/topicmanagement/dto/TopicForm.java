package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dữ liệu form giảng viên đăng ký / sửa đề tài. */
public class TopicForm {

    @NotBlank(message = "Vui lòng nhập tên đề tài")
    @Size(max = 500, message = "Tên đề tài tối đa 500 ký tự")
    private String title;

    @NotBlank(message = "Vui lòng mô tả nội dung đề tài")
    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn bộ môn")
    private Long departmentId;

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;

    // Không bắt buộc: GVHD thứ hai (đề tài có tối đa 2 GVHD, người tạo là GVHD chính)
    private Long coSupervisorId;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getDepartmentId() { return departmentId; }
    public Long getPeriodId() { return periodId; }
    public Long getCoSupervisorId() { return coSupervisorId; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public void setCoSupervisorId(Long coSupervisorId) { this.coSupervisorId = coSupervisorId; }
}
