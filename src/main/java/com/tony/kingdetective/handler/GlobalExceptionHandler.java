package com.tony.kingdetective.handler;

import com.oracle.bmc.model.BmcException;
import com.tony.kingdetective.bean.vo.ErrorResponse;
import com.tony.kingdetective.exception.OciException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     *  OCI 
     */
    @ExceptionHandler(OciException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleOciException(OciException e) {
        log.error("OCI ????: code={}, message={}", e.getCode(), e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        try {
            if (e.getCode() >= 100 && e.getCode() <= 599) {
                status = HttpStatus.valueOf(e.getCode());
            }
        } catch (IllegalArgumentException ex) {
            // Fallback to BAD_REQUEST if code is not a valid HTTP status
        }
        
        return ResponseEntity
                .status(status)
                .body(response);
    }
    
    /**
     *  Oracle Cloud SDK 
     */
    @ExceptionHandler(BmcException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleBmcException(BmcException e) {
        log.error("Oracle Cloud SDK ??: statusCode={}, serviceCode={}, message={}",
                e.getStatusCode(), e.getServiceCode(), e.getMessage(), e);
        
        String userMessage = formatBmcExceptionMessage(e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatusCode())
                .message(userMessage)
                .details(e.getServiceCode())
                .timestamp(System.currentTimeMillis())
                .build();
        
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
        
        return ResponseEntity
                .status(status)
                .body(response);
    }
    
    /**
     * 
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("??????: {}", e.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(400)
                .message("??????: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * 
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        log.error("???????", e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(500)
                .message("???????????")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation failed: {}", message);

        ErrorResponse response = ErrorResponse.builder()
                .code(400)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    /**
     * 
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("????????", e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(500)
                .message("???????" + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    
    /**
     *  BMC 
     */
    private String formatBmcExceptionMessage(BmcException e) {
        int statusCode = e.getStatusCode();
        String serviceCode = e.getServiceCode();
        
        // 
        if (statusCode == 401) {
            return "???????? API ????????";
        } else if (statusCode == 404) {
            return "??????????? ID ????";
        } else if (statusCode == 429) {
            return "????????????";
        } else if (statusCode == 500) {
            return "Oracle Cloud ???????????";
        } else if (statusCode == 503) {
            return "Oracle Cloud ?????????????";
        } else if ("LimitExceeded".equals(serviceCode)) {
            return "???????????????";
        } else if ("InsufficientHostCapacity".equals(serviceCode)) {
            return "?????????????????";
        } else {
            return e.getMessage();
        }
    }
}
