package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.FeedbackDto;
import com.hyp.entity.Feedback;
import com.hyp.response.Response;
import com.hyp.service.FeedbackService;
import com.hyp.translation.FeedbackTranslation;

@RestController
@RequestMapping("/feedback")
public class FeedbackController extends BaseController<FeedbackDto, Feedback, String> {

	@Value("${chat.io.url}")
	private String url;

	@Autowired
	public FeedbackTranslation feedbackTranslation;

	@Autowired
	FeedbackService feedbackService;

	@GetMapping("/consume")
	public ResponseEntity<Response> get() {
		Response response;
		try {
			feedbackService.getDataFromChatIO(url);
			response = new Response(null, false, "Feedback Successfully Consumed from ChatIO");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}