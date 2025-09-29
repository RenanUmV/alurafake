package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByStatement(String statement);

    boolean existsByOrderAndCourse(Integer order, Course course);

    List<Task> findByCourseAndOrderGreaterThanEqualOrderByOrderAsc(Course course, Integer order);

}
