package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Dữ liệu tạo hội đồng cho một đợt TLCN/KLTN. */
public class CouncilForm {
    @NotBlank(message = "Vui lòng nhập tên hội đồng")
    @Size(max = 255, message = "Tên hội đồng tối đa 255 ký tự")
    private String name;
    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime defenseDate;
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String room;
    public String getName() { return name; }
    public Long getPeriodId() { return periodId; }
    public LocalDateTime getDefenseDate() { return defenseDate; }
    public String getRoom() { return room; }
    public void setName(String name) { this.name = name; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public void setDefenseDate(LocalDateTime defenseDate) { this.defenseDate = defenseDate; }
    public void setRoom(String room) { this.room = room; }
}
