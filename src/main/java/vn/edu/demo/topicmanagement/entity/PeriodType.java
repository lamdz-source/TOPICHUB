package vn.edu.demo.topicmanagement.entity;

/** Loại đợt đăng ký, khớp với ENUM trong registration_periods.type. */
public enum PeriodType {
    COURSE("Đề tài môn học"),
    RESEARCH("Nghiên cứu khoa học (NCKH)"),
    TLCN("Tiểu luận chuyên ngành (TLCN)"),
    KLTN("Khóa luận tốt nghiệp (KLTN)");

    private final String displayName;

    PeriodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
