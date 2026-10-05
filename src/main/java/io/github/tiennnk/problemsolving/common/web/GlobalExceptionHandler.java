package io.github.tiennnk.problemsolving.common.web;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.github.tiennnk.problemsolving.common.llm.LlmException;
import io.github.tiennnk.problemsolving.task.application.TaskNotFoundException;
import io.github.tiennnk.problemsolving.task.domain.InvalidTaskTransitionException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TaskNotFoundException.class)
  ProblemDetail handleNotFound(TaskNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            error -> String.valueOf(error.getDefaultMessage()),
            (first, second) -> first));
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
    if (ex.getMostSpecificCause() instanceof InvalidFormatException invalid
        && invalid.getTargetType() != null
        && invalid.getTargetType().isEnum()) {
      String field = invalid.getPath().stream()
          .map(JacksonException.Reference::getPropertyName)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("."));
      String detail = "Invalid value '%s' for field '%s'. Allowed values: %s".formatted(
          invalid.getValue(), field, Arrays.toString(invalid.getTargetType().getEnumConstants()));
      return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String detail = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
  }

  @ExceptionHandler(InvalidTaskTransitionException.class)
  ProblemDetail handleInvalidTransition(InvalidTaskTransitionException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(LlmException.class)
  ProblemDetail handleLlmFailure(LlmException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Task was modified concurrently, please retry");
  }
}
