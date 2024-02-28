package com.hyp.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.request.PosDataRequest;
import com.hyp.response.Response;
import com.hyp.service.PosDataService;

@RestController
@RequestMapping("/api/pos/menu")
public class PosDataController {

	@Autowired
	PosDataService posDataService;

	@PostMapping()
	public ResponseEntity<Response> save(@RequestBody PosDataRequest posDataRequest) {
		JSONObject jb = new JSONObject(posDataRequest);
		System.out.println(jb.toString());
		boolean result = posDataService.savePosData(posDataRequest);
		Response response = new Response.Builder()
				.httpCode(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Menu Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}
}
