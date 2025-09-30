package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.option.Option;
import br.com.alura.AluraFake.course.Course;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.List;

public class NewTaskDTO {

    @NotNull
    @NotBlank
    @Length(min = 4, max = 255)
    private String statement;
    @NotNull
    private Integer order;
    @NotNull
    private Long courseId;

    @Valid
    private List<Option> options = new ArrayList<>();

    public NewTaskDTO() {}

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Task toModel(Type type, Course course){
        return new Task(statement, type, order, course, options);
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }
}
