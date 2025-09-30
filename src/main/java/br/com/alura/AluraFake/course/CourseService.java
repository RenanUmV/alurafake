package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.task.TaskRepository;
import br.com.alura.AluraFake.task.Type;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Transactional
    public Course publishCourse(Long courseId){
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ValidationException("course| Curso não encontrado com o ID:" + courseId));

        if (!course.getStatus().equals(Status.BUILDING)){
            throw new ValidationException("status|O Curso só pode ser publicado se seu status for BUILING." +
                    " Status atual: " + course.getStatus());
        }

        validateOrderContinuity(courseId);

        validateTaskTypeCoverage(courseId);

        course.setStatus(Status.PUBLISHED);

        return courseRepository.save(course);
    }

    private void validateTaskTypeCoverage(Long courseId){
        List<Object[]> typeCounts = taskRepository.countTasksByTypeForCourse(courseId);

        Map<Type, Long> countsMap = typeCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (Type) arr[0],
                        arr -> (Long) arr[1]
                ));

        for(Type type : Type.values()){
            if (!countsMap.containsKey(type) || countsMap.get(type) < 1){
                throw new ValidationException("task|O curso deve conter ao menos uma atividade do tipo: " + type.name());
            }
        }
    }

    private void validateOrderContinuity(Long courseId){
        List<Integer> orders = taskRepository.findAllOrdersByCourseId(courseId);

        if (orders.isEmpty()) {
            throw new ValidationException("tasks|O curso não pode ser publicado sem nenhuma atividade.");
        }

        if (orders.get(0) != 1) {
            throw new ValidationException("order|A sequência de atividades deve começar na ordem 1.");
        }

        for (int i = 0; i < orders.size(); i++) {
            int expectedOrder = i + 1;

            if (orders.get(i) != expectedOrder) {
                throw new ValidationException(
                        "order|Sequência de ordem não é contínua. Salto encontrado na ordem " + expectedOrder + "."
                );
            }
        }
    }
}
