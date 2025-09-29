package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    public Task createTask(NewTaskDTO newTaskDTO){

        Course course = courseRepository.findById(newTaskDTO.getCourseId())
                .orElseThrow(()-> new IllegalArgumentException("Curso não encontrado com o ID: " + newTaskDTO.getCourseId()));

        this.incrementExistingOrders(course, newTaskDTO.getOrder());

        Task newTask = newTaskDTO.toModel(Type.OPEN_TEXT, course);

        course.addTask(newTask);

        return taskRepository.save(newTask);
    }

    private void incrementExistingOrders(Course course, Integer order){

        List<Task> tasksToUpdate = taskRepository.findByCourseAndOrderGreaterThanEqualOrderByOrderAsc(course, order);

        if(!tasksToUpdate.isEmpty()){
            for(Task task : tasksToUpdate){
                task.setOrder(task.getOrder() + 1);
            }
        }

        taskRepository.saveAll(tasksToUpdate);
    }
}
