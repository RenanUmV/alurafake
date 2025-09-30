package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>{

    List<Course> findByAuthor(User author);

}
