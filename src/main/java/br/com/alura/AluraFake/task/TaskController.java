package br.com.alura.AluraFake.task;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

    @Autowired
    private TaskService taskService;


    @Transactional
    @PostMapping("/task/new/opentext")
    public ResponseEntity newOpenTextExercise(@Valid @RequestBody NewTaskDTO newTask) {
        taskService.createOpenTextTask(newTask);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/task/new/singlechoice")
    public ResponseEntity newSingleChoice(@Valid @RequestBody NewTaskDTO newTask) {
        taskService.createSingleChoiceTask(newTask);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/task/new/multiplechoice")
    public ResponseEntity newMultipleChoice(@Valid @RequestBody NewTaskDTO newTask) {
        taskService.createMultipleChoiceTask(newTask);
        return ResponseEntity.ok().build();
    }
}