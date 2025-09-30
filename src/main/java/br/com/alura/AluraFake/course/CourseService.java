package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.task.TaskRepository;
import br.com.alura.AluraFake.task.Type;
import br.com.alura.AluraFake.user.User;
import br.com.alura.AluraFake.user.UserInstructorCourseReportDTO;
import br.com.alura.AluraFake.user.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

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

    public UserInstructorCourseReportDTO generateInstructorReport(Long instructorId){

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario nao encontrado: "
                        + instructorId));

        if(!instructor.isInstructor()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario de ID: " + instructorId + "nao é um" +
                    " instrutor");
        }

        List<Course> courses = courseRepository.findByAuthor(instructor);

        Map<Long, Integer> taskCounts = taskRepository.countTasksByCourseForInstructor(instructorId).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> ((Long) arr[1]).intValue()
                ));

        List<CourseReportItemDTO> reportItems = courses.stream().map(
                course -> new CourseReportItemDTO(
                        course,
                        taskCounts.getOrDefault(course.getId(), 0)
                ))
                .toList();

        long totalPublished = courses.stream()
                .filter(course -> course.getStatus().equals(Status.PUBLISHED))
                .count();

        return new UserInstructorCourseReportDTO(instructor.getName(), totalPublished, reportItems);
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
