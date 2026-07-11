package com.edu.archive.config;


// 【全局异常处理器】统一处理参数校验异常(400)、权限异常(403)、数据完整性异常(400)、通用异常(500)。
import com.edu.archive.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<?> handleDenied(AccessDeniedException e) {
        return ApiResponse.fail(403, "无权限访问");
    }

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public ApiResponse<?> handleDuplicate(DuplicateKeyException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return ApiResponse.fail(400, "数据重复或违反完整性约束，请检查输入");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handle(Exception e) {
        log.error("Unhandled exception", e);
        String msg = e.getMessage();
        if (msg == null && e.getCause() != null) {
            msg = e.getCause().getMessage();
        }
        if (msg == null) {
            msg = e.getClass().getSimpleName();
        }
        return ApiResponse.fail(500, msg);
    }
}
