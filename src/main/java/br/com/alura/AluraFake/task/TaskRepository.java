package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByStatement(String statement);

    boolean existsByOrderAndCourse(Integer order, Course course);

}
