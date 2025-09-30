package br.com.alura.AluraFake.task;

import br.com.alura.AluraFake.course.Course;
import br.com.alura.AluraFake.course.CourseRepository;
import br.com.alura.AluraFake.option.Option;
import br.com.alura.AluraFake.user.Role;
import br.com.alura.AluraFake.user.User;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CourseRepository courseRepository;

    private Course validCourse;
    private Long courseId;
    private NewTaskDTO openTextDTO;
    private NewTaskDTO singleChoiceDTO;
    private NewTaskDTO multipleChoiceDTO;

    @BeforeEach
    void setUp(){
        User instructor = new User("Instrutor Test", "instrutor@alura.com", Role.INSTRUCTOR);
        validCourse = new Course("Curso Teste", "Descricao", instructor);
        courseId = validCourse.getId();

        lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.of(validCourse));
        lenient().when(taskRepository.existsByStatement(anyString())).thenReturn(false);
        lenient().when(taskRepository.findMaxOrderForCourse(courseId)).thenReturn(null);

        // -- OPEN TEXT --
        openTextDTO = new NewTaskDTO();
        openTextDTO.setCourseId(courseId);
        openTextDTO.setStatement("O que voce achou da aula de hoje?");
        openTextDTO.setOrder(1);

        // -- SINGLE CHOICE --
        singleChoiceDTO = new NewTaskDTO();
        singleChoiceDTO.setCourseId(courseId);
        singleChoiceDTO.setStatement("Qual linguagem aprendemos hoje?");
        singleChoiceDTO.setOrder(1);
        singleChoiceDTO.setOptions(List.of(
                new Option("Java", true),
                new Option("Python", false),
                new Option("Javascript", false)
        ));

        // -- MULTIPLE CHOICE --
        multipleChoiceDTO = new NewTaskDTO();
        multipleChoiceDTO.setCourseId(courseId);
        multipleChoiceDTO.setStatement("Quais sao frameworks Java?");
        multipleChoiceDTO.setOrder(1);
        multipleChoiceDTO.setOptions(List.of(
                new Option("Spring", true),
                new Option("Hibernate", true),
                new Option("Node", false),
                new Option("Django", false)
        ));
    }

    @Nested
    @DisplayName("Criancao e Ordenacao")
    class CreationAndOrderingTests {

        @Test
        @DisplayName("Sucesso: Deve criar OPEN_TEXT, SINGLE_CHOICE e MULTIPLE_CHOICE")
        void shouldCreateAllTasksTypeSuccessfully(){
            //OPEN_TEXT
            assertDoesNotThrow(()-> taskService.createOpenTextTask(openTextDTO), "OPEN_TEXT falhou na criação.");
            //SINGLE_CHOICE
            assertDoesNotThrow(()-> taskService.createSingleChoiceTask(singleChoiceDTO), "SINGLE_CHOICE falhou na criação.");
            //MULTIPLE_CHOICE
            assertDoesNotThrow(()-> taskService.createMultipleChoiceTask(multipleChoiceDTO), "MULTIPLE_CHOICE falhou na criação.");

            verify(taskRepository, times(3)).save(any(Task.class));
        }

        @Test
        @DisplayName("Ordenacao: Deve falhar em qualquer tipo se a continuidade da ordem for quebrada (Gaps).")
        void shouldFailIfOrderContinuityIsBrokenForAnyType(){
            openTextDTO.setOrder(5);
            when(taskRepository.findMaxOrderForCourse(courseId)).thenReturn(3);
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    taskService.createOpenTextTask(openTextDTO));

            assertTrue(exception.getMessage().contains("A próxima ordem esperada é 4"));
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Validacao Base: Deve falhar em qualquer tipo se o statement já existir.")
        void shouldFailIfStatementAlreadyExistsForAnyType(){
            when(taskRepository.existsByStatement(anyString())).thenReturn(true);

            ValidationException exception = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );

            assertTrue(exception.getMessage().contains("Ja existe uma atividade com este titulo"));
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("Validação Single Choice")
    class SingleChoiceValidationTests {

        @Test
        @DisplayName("SINGLE_CHOICE: Deve falhar se tiver zero ou mais de uma opção correta.")
        void shouldFailIfHasZeroOrMultipleCorrectOptions() {
            // Cenário 1: Zero corretas
            singleChoiceDTO.setOptions(List.of(new Option("Python", false), new Option("Javascript", false)));

            ValidationException exZero = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );
            assertTrue(exZero.getMessage().contains("apenas uma alternativa correta"));

            // Cenário 2: Duas corretas
            singleChoiceDTO.setOptions(List.of(new Option("Python", true), new Option("Javascript", true)));

            ValidationException exTwo = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );
            assertTrue(exTwo.getMessage().contains("apenas uma alternativa correta"));
        }

        @Test
        @DisplayName("SINGLE_CHOICE: Deve falhar se tiver menos de 2 ou mais de 5 opções.")
        void shouldFailIfOptionCountIsOutOfRange() {
            // Cenário 1: Menos de 2
            singleChoiceDTO.setOptions(List.of(new Option("Java", true)));

            ValidationException exMin = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );
            assertTrue(exMin.getMessage().contains("entre 2 e 5 alternativas"));

            // Cenário 2: Mais de 5
            singleChoiceDTO.setOptions(List.of(
                    new Option("Java", true), new Option("Javascript", false), new Option("Python", false),
                    new Option("Django", false), new Option("Mysql", false), new Option("HTML", false)
            ));

            ValidationException exMax = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );
            assertTrue(exMax.getMessage().contains("entre 2 e 5 alternativas"));
        }
    }

    @Nested
    @DisplayName("Validação Multiple Choice")
    class MultipleChoiceValidationTests {

        @Test
        @DisplayName("MC: Deve falhar se tiver menos de 2 opções corretas.")
        void shouldFailIfLessThanTwoCorrectOptions() {
            multipleChoiceDTO.setOptions(List.of(
                    new Option("Spring", true), // 1 Correta
                    new Option("Hibernate", false),
                    new Option("Node", false)
            ));

            ValidationException exception = assertThrows(ValidationException.class, () ->
                    taskService.createMultipleChoiceTask(multipleChoiceDTO)
            );

            assertTrue(exception.getMessage().contains("pelo menos duas alternativas corretas"));
        }

        @Test
        @DisplayName("MC: Deve falhar se não tiver ao menos uma incorreta.")
        void shouldFailIfNoIncorrectOptions() {
            multipleChoiceDTO.setOptions(List.of(
                    new Option("Spring", true),
                    new Option("Hibernate", true),
                    new Option("OSGI", true) // Todas corretas
            ));

            ValidationException exception = assertThrows(ValidationException.class, () ->
                    taskService.createMultipleChoiceTask(multipleChoiceDTO)
            );

            assertTrue(exception.getMessage().contains("pelo menos uma alternativa incorreta"));
        }

        @Test
        @DisplayName("MC: Deve falhar se tiver menos de 3 ou mais de 5 opções.")
        void shouldFailIfOptionCountIsOutOfRange() {
            // Cenário 1: Menos de 3
            multipleChoiceDTO.setOptions(List.of(new Option("Spring", true), new Option("MySQL", false)));

            ValidationException exMin = assertThrows(ValidationException.class, () ->
                    taskService.createMultipleChoiceTask(multipleChoiceDTO)
            );
            assertTrue(exMin.getMessage().contains("entre 3 e 5 alternativas"));
        }
    }

    @Nested
    @DisplayName("Validação de Opções Comuns")
    class CommonOptionRulesTests {

        @Test
        @DisplayName("Comum: Deve falhar se houver títulos duplicados (Case Insensitive) no SC.")
        void shouldFailIfOptionTitlesAreDuplicated() {
            singleChoiceDTO.setOptions(List.of(
                    new Option("Java", true),
                    new Option("java", false) // Duplicata case insensitive
            ));

            ValidationException exception = assertThrows(ValidationException.class, () ->
                    taskService.createSingleChoiceTask(singleChoiceDTO)
            );

            assertTrue(exception.getMessage().contains("As alternativas não podem possuir o mesmo título"));
        }
    }
}
