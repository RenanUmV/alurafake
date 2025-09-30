package br.com.alura.AluraFake.user;

import br.com.alura.AluraFake.course.CourseReportItemDTO;

import java.util.List;

public class UserInstructorCourseReportDTO {

    private String name;
    private long totalPublishedCourses;
    private List<CourseReportItemDTO> courses;

    public UserInstructorCourseReportDTO(String name, long totalPublishedCourses, List<CourseReportItemDTO> courses) {
        this.name = name;
        this.totalPublishedCourses = totalPublishedCourses;
        this.courses = courses;
    }

    public String getName() {
        return name;
    }

    public long getTotalPublishedCourses() {
        return totalPublishedCourses;
    }

    public List<CourseReportItemDTO> getCourses() {
        return courses;
    }
}
