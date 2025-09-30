package br.com.alura.AluraFake.course;

import java.time.LocalDateTime;

public class CourseReportItemDTO {

    private Long id;
    private String title;
    private Status status;
    private LocalDateTime publishedAt;
    private int taskCount;

    public CourseReportItemDTO(Course course, int taskCount) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.status = course.getStatus();
        this.publishedAt = course.getPublishedAt();
        this.taskCount = taskCount;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getTaskCount() {
        return taskCount;
    }
}
