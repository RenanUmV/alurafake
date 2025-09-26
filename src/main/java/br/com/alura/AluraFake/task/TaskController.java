package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import br.com.alura.AluraFake.util.ErrorItemDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class TaskController {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public TaskController(TaskRepository taskRepository, CourseRepository courseRepository) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
    }


    @Transactional
    @PostMapping("/task/new/opentext")
    public ResponseEntity newOpenTextExercise(@Valid @RequestBody NewTaskDTO newTask) {
        if (taskRepository.existsByStatement(newTask.getStatement())){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorItemDTO("statement", "Ja existe uma atividade com este titulo "));
        }

        Optional<Course> course = courseRepository.findById(newTask.getCourseId());

        if(course.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorItemDTO("courseId", "Não foi encontrado curso com este ID"));
        }

        Task task = newTask.toModel(Type.OPEN_TEXT, course.get());

        taskRepository.save(task);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/task/new/singlechoice")
    public ResponseEntity newSingleChoice() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/task/new/multiplechoice")
    public ResponseEntity newMultipleChoice() {
        return ResponseEntity.ok().build();
    }

}