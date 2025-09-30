package br.com.alura.AluraFake.course;

import br.com.alura.AluraFake.task.TaskRepository;
import br.com.alura.AluraFake.task.Type;
import br.com.alura.AluraFake.user.Role;
import br.com.alura.AluraFake.user.User;
import br.com.alura.AluraFake.user.UserInstructorCourseReportDTO;
import br.com.alura.AluraFake.user.UserRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;

    private Long courseId = 10L;
    private Long instructorId = 1L;
    private Course courseBuilding;
    private User instructor;

    @BeforeEach
    void setUp() {
        instructor = new User("Instrutor Teste", "instrutor@alura.com", Role.INSTRUCTOR);
        courseBuilding = new Course("Curso em Construção", "Descricao", instructor);
        courseBuilding.setStatus(Status.BUILDING);

        lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseBuilding));
    }

    private void injectId(Course course, Long id) {
        try {
            java.lang.reflect.Field idField = Course.class.getDeclaredField("id");

            idField.setAccessible(true);

            idField.set(course, id);

            idField.setAccessible(false);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Falha ao injetar ID usando Reflexão. Verifique o nome do campo 'id' na entidade Course.", e);
        }
    }

    @Nested
    @DisplayName("Publicação de Curso (publishCourse)")
    class PublishCourseTests {

        private void mockSuccessConditions() {
            lenient().when(taskRepository.findAllOrdersByCourseId(courseId)).thenReturn(List.of(1, 2));

            lenient().when(taskRepository.countTasksByTypeForCourse(courseId)).thenReturn(List.of(
                    new Object[]{Type.OPEN_TEXT, 1L},
                    new Object[]{Type.SINGLE_CHOICE, 1L},
                    new Object[]{Type.MULTIPLE_CHOICE, 1L}
            ));
        }

        @Test
        @DisplayName("Sucesso: Deve publicar o curso e atualizar status/data.")
        void shouldPublishCourseSuccessfully() {
            mockSuccessConditions();
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

            Course published = courseService.publishCourse(courseId);

            assertEquals(Status.PUBLISHED, published.getStatus());
            assertNotNull(published.getPublishedAt());
            verify(courseRepository, times(1)).save(published);
        }

        @Test
        @DisplayName("Falha: Status: Deve lançar exceção se o status NÃO for BUILDING.")
        void shouldFailIfStatusIsNotBuilding() {
            courseBuilding.setStatus(Status.PUBLISHED); // Curso já publicado

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                courseService.publishCourse(courseId);
            });

            assertTrue(exception.getMessage().contains("só pode ser publicado se seu status for BUILDING"));
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falha: Tipos: Deve lançar exceção se faltar algum tipo de atividade.")
        void shouldFailIfTaskTypeCoverageIsMissing() {
            lenient().when(taskRepository.findAllOrdersByCourseId(courseId)).thenReturn(List.of(1, 2, 3));

            when(taskRepository.countTasksByTypeForCourse(courseId)).thenReturn(List.of(
                    new Object[]{Type.SINGLE_CHOICE, 1L},
                    new Object[]{Type.MULTIPLE_CHOICE, 1L}
            ));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                courseService.publishCourse(courseId);
            });

            assertTrue(exception.getMessage().contains("deve conter ao menos uma atividade do tipo: OPEN_TEXT"));
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falha: Ordem: Deve lançar exceção se a sequência não for contínua (Salto).")
        void shouldFailIfOrderIsNotContinuous() {
            // Ordem com salto: 1, 3 (falta o 2)
            when(taskRepository.findAllOrdersByCourseId(courseId)).thenReturn(List.of(1, 3));

            ValidationException exception = assertThrows(ValidationException.class, () -> {
                courseService.publishCourse(courseId);
            });

            assertTrue(exception.getMessage().contains("Salto encontrado na ordem 2."));
            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Relatório de Instrutor (generateInstructorReport)")
    class InstructorReportTests {

        private List<Course> instructorCourses;

        @BeforeEach
        void setupReport() {
            // Cursos mockados: 2 publicados, 1 em construção
            Course course1 = new Course("Java Básico", "Desc", instructor);
            course1.setStatus(Status.PUBLISHED);
            Course course2 = new Course("Spring Avançado", "Desc", instructor);
            course2.setStatus(Status.BUILDING);
            Course course3 = new Course("SQL", "Desc", instructor);
            course3.setStatus(Status.PUBLISHED);

            injectId(course1, 20L);
            injectId(course2, 21L);
            injectId(course3, 22L);

            instructorCourses = List.of(course1, course2, course3);

            lenient().when(userRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
            lenient().when(courseRepository.findByAuthor(instructor)).thenReturn(instructorCourses);

            lenient().when(taskRepository.countTasksByCourseForInstructor(instructorId)).thenReturn(List.of(
                    new Object[]{20L, 5L}, // Course 20 tem 5 tasks
                    new Object[]{21L, 2L}  // Course 21 tem 2 tasks
            ));
        }

        @Test
        @DisplayName("Sucesso: Deve gerar relatório com total de publicados e contagem de atividades correta.")
        void shouldGenerateReportWithCorrectAggregations() {
            UserInstructorCourseReportDTO report = courseService.generateInstructorReport(instructorId);

            assertEquals(2, report.getTotalPublishedCourses());

            Map<Long, Integer> taskCounts = report.getCourses().stream()
                    .collect(Collectors.toMap(CourseReportItemDTO::getId, CourseReportItemDTO::getTaskCount));

            assertEquals(3, report.getCourses().size());
            assertEquals(5, taskCounts.get(20L)); // Java Básico
            assertEquals(2, taskCounts.get(21L)); // Spring Avançado
            assertEquals(0, taskCounts.get(22L)); // SQL (sem contagem no mock, deve ser 0 por default)
        }

        @Test
        @DisplayName("Sucesso: Deve retornar lista vazia se o instrutor não tiver cursos.")
        void shouldReturnEmptyListIfInstructorHasNoCourses() {
            when(courseRepository.findByAuthor(instructor)).thenReturn(Collections.emptyList());

            UserInstructorCourseReportDTO report = courseService.generateInstructorReport(instructorId);

            assertTrue(report.getCourses().isEmpty());
            assertEquals(0, report.getTotalPublishedCourses());
        }

        @Test
        @DisplayName("Falha: Usuário: Deve lançar 404 se o ID do usuário não existir.")
        void shouldFailIfUserDoesNotExist() {
            when(userRepository.findById(instructorId)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                courseService.generateInstructorReport(instructorId);
            });

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Usuario nao encontrado"));
        }

        @Test
        @DisplayName("Falha: Usuário: Deve lançar 400 se o usuário existir mas NÃO for instrutor.")
        void shouldFailIfUserIsNotInstructor() {
            User nonInstructor = new User("Aluno", "aluno@alura.com", Role.STUDENT); // isInstructor = false
            when(userRepository.findById(instructorId)).thenReturn(Optional.of(nonInstructor));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                courseService.generateInstructorReport(instructorId);
            });

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertTrue(exception.getReason().contains("nao é um instrutor"));
        }
    }
}
