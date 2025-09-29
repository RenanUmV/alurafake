package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import br.com.alura.AluraFake.option.Option;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Transactional
    public Task createOpenTextTask(NewTaskDTO newTaskDTO) {
        validateBaseTask(newTaskDTO);

        return saveTask(newTaskDTO, Type.OPEN_TEXT);
    }

    @Transactional
    public Task createSingleChoiceTask(NewTaskDTO newTaskDTO) {
        validateBaseTask(newTaskDTO);
        validateSingleChoiceOptions(newTaskDTO);

        return saveTask(newTaskDTO, Type.SINGLE_CHOICE);
    }

    @Transactional
    public Task createMultipleChoiceTask(NewTaskDTO newTaskDTO) {
        validateBaseTask(newTaskDTO);
        validateMultipleChoiceOptions(newTaskDTO);

        return saveTask(newTaskDTO, Type.MULTIPLE_CHOICE);
    }

    private void validateSingleChoiceOptions(NewTaskDTO newTaskDTO) {
        List<Option> options = newTaskDTO.getOptions();

        if (options == null) {
            throw new ValidationException("options|O corpo da requisição não contém opções.");
        }

        if (options.size() < 2 || options.size() > 5){
            throw new ValidationException("options|Deve ter entre 2 e 5 alternativas");
        }

        int correctCount = validateOptions(newTaskDTO);

        if (correctCount != 1){
            throw new ValidationException("options|Deve conter apenas uma alternativa correta");
        }
    }

    private void validateMultipleChoiceOptions(NewTaskDTO newTaskDTO) {
        List<Option> options = newTaskDTO.getOptions();

        if (options.size() < 3 || options.size() > 5){
            throw new ValidationException("options|Deve ter entre 3 e 5 alternativas");
        }

        int correctCount = validateOptions(newTaskDTO);
        int incorrectCount = options.size() - correctCount;

        if (correctCount < 2){
            throw new ValidationException("options|Deve conter pelo menos duas alternativas corretas.");
        }

        if (incorrectCount < 1){
            throw new ValidationException("options|Deve conter pelo menos uma alternativa incorreta.");
        }
    }

    private int validateOptions(NewTaskDTO newTaskDTO) {
        Set<String> uniqueTitles = new HashSet<>();
        int correctCount = 0;

        for(Option option : newTaskDTO.getOptions()){
            String title = option.getOption().trim();
            String lowerCaseTitle = title.toLowerCase();

            if(title.equals(newTaskDTO.getStatement())){
                throw new ValidationException("option|A alternativa não pode ter o mesmo título do enunciado");
            }

            if (uniqueTitles.contains(lowerCaseTitle)){
                throw new ValidationException("options|As alternativas não podem possuir o mesmo título");
            }
            uniqueTitles.add(lowerCaseTitle);

            if (option.getIsCorrect()){
                correctCount ++;
            }
        }
        return correctCount;
    }

    private Task saveTask(NewTaskDTO newTaskDTO, Type type){

        Course course = courseRepository.findById(newTaskDTO.getCourseId())
                .orElseThrow(()-> new ValidationException("courseId|Não foi encontrado curso com este ID"));

        validateOrderContinuity(course, newTaskDTO.getOrder());

        this.incrementExistingOrders(course, newTaskDTO.getOrder());

        Task newTask = newTaskDTO.toModel(type, course);

        course.addTask(newTask);

        return taskRepository.save(newTask);
    }

    private void validateBaseTask(NewTaskDTO newTaskDTO) {
        if (taskRepository.existsByStatement(newTaskDTO.getStatement())){
            throw new ValidationException("statement|Ja existe uma atividade com este titulo");
        }
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

    private void validateOrderContinuity(Course course, Integer desiredOrder) {
        Integer maxOrder = taskRepository.findMaxOrderForCourse(course.getId());

        if (maxOrder == null) {
            if (desiredOrder != 1) {
                throw new ValidationException(
                        "order|A primeira atividade deve ter ordem 1, mas foi solicitada a ordem " + desiredOrder
                );
            }
            return;
        }

        Integer nextExpectedOrder = maxOrder + 1;

        if (desiredOrder > nextExpectedOrder) {
            throw new ValidationException(
                    "order|Sequência de ordem inválida. A próxima ordem esperada é " + nextExpectedOrder +
                            ", mas foi solicitada a ordem " + desiredOrder
            );
        }
    }

}
