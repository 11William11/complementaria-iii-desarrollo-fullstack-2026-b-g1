package co.edu.corhuila.barflow.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Traduce las excepciones de las capas internas a respuestas HTTP. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(ProductNotFoundException ex) {
        return ApiError.of("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError duplicate(DuplicateProductException ex) {
        return ApiError.of("CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidBody(MethodArgumentNotValidException ex) {
        List<ApiError.FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.FieldDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return new ApiError("VALIDATION_ERROR", "the request body is not valid", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError unreadableBody() {
        return ApiError.of("MALFORMED_REQUEST", "the request body is not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError badPathParameter(MethodArgumentTypeMismatchException ex) {
        return ApiError.of("MALFORMED_REQUEST", ex.getName() + " must be a valid UUID");
    }
}
