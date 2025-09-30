package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByStatement(String statement);

    boolean existsByOrderAndCourse(Integer order, Course course);

    List<Task> findByCourseAndOrderGreaterThanEqualOrderByOrderAsc(Course course, Integer order);

    @Query("SELECT MAX(t.order) FROM Task t WHERE t.course.id = :courseId")
    Integer findMaxOrderForCourse(@Param("courseId")Long courseId);

    @Query("SELECT t.type, COUNT(t) FROM Task t WHERE t.course.id = :courseId GROUP BY t.type")
    List<Object[]> countTasksByTypeForCourse(Long courseId);

    @Query("SELECT t.order FROM Task t WHERE t.course.id = :courseId ORDER BY t.order ASC")
    List<Integer> findAllOrdersByCourseId(Long courseId);

}
