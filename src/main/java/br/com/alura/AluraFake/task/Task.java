package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.option.Option;
import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.Status;
import jakarta.persistence.*;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String statement;
    @Enumerated(EnumType.STRING)
    private Type type;
    @Column(name = "task_order")
    private Integer order;
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options;

    @Deprecated
    public Task(){}

    public Task(String statement, Type type, Integer order, Course course, List<Option> options) {
        Assert.isTrue(course.getStatus().equals(Status.BUILDING), "Curso deve ter status BUILDING");
        this.statement = statement;
        this.type = type;
        this.order = order;
        this.course = course;
        this.options = options;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatement() {
        return statement;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public void setCourse(Course course){
        course = course;
    }

    public void addOption(Option option) {
        this.options.add(option);
        option.setTask(this);
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
