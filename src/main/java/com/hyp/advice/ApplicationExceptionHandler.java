package com.hyp.advice;

import java.util.List;
import java.util.stream.Collectors;

import com.hyp.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hyp.constants.ErrorConstants;
import com.hyp.response.Response;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ApplicationExceptionHandler {

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(DeliveryException.class)
	public ResponseEntity<Response> handleGlobalDeliveryException(DeliveryException ex) {
		log.error("Exception occurred in Delivery Service {} ",ex.getMessage());
		Response response = new Response(null, true, ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
	
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(OneSignalException.class)
	public ResponseEntity<Response> handleGlobalOneSignalException(OneSignalException ex) {
		log.error("Exception occurred in One Signal Service {}",ex.getMessage());
		Response response = new Response(null, true, ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Response> handleEntityNotFoundException(EntityNotFoundException ex) {
		log.error("Entity not found in {} for {} ",ex.getEntityName(), ex.getEntityValue());
		Response response = new Response(null, true, ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Response> handleBadRequestException(BadRequestException ex) {
		log.error("Bad request on {} for {} ", ex.getEntity(), ex.getMessage());
		Response response = new Response(null, true, ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(PaymentException.class)
	public ResponseEntity<Response> handlePaymentException(PaymentException ex) {
		log.error("Exception occurred in Payment Service {}",ex.getMessage());
		Response response = new Response(null, true, ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                                .collect(Collectors.toList());

        Response response = new Response(errors, true, "Validation Failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}