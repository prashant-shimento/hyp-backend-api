package com.hyp.request;

import java.util.List;

import lombok.Data;

@Data
public class PredictionRequest {

	String input;
	List<String> includedRegionCodes;
	
}
