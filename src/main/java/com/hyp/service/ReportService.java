package com.hyp.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.Report;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.ReportRepository;
import com.hyp.request.ReportRequest;

import lombok.extern.slf4j.Slf4j;

import org.bson.Document;

@Service
@Slf4j
public class ReportService extends BaseServiceImpl<Report, String> {

	@Autowired
	ReportRepository reportRepository;
	@Autowired
	private MongoTemplate mongoTemplate;

	public List<Document> executeReport(Report report, ReportRequest reportRequest) throws EntityNotFoundException, BadRequestException {
		List<Document> query = buildQueryWithParams(report.getQuery(), reportRequest.getParameters());
		AggregationOperation[] operations = query.stream()
				.map(stage -> (AggregationOperation) context -> new org.bson.Document(stage))
				.toArray(AggregationOperation[]::new);
		Aggregation aggregation = Aggregation.newAggregation(operations);
		return mongoTemplate.aggregate(aggregation, report.getCollectionName(), Document.class).getMappedResults();
	}

	private List<Document> buildQueryWithParams(List<Map<String, Object>> storedQuery, Map<String, Object> parameters) throws BadRequestException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Document> queryList = new ArrayList<>();

		for (Map<String, Object> queryStage : storedQuery) {
			String jsonString;
			try {
				jsonString = objectMapper.writeValueAsString(queryStage);
				
				validateParameters(jsonString, parameters);				
				
				for (Map.Entry<String, Object> param : parameters.entrySet()) {
					String key = "{{" + param.getKey() + "}}";
					if (param.getValue() instanceof List) {
						String listJson = objectMapper.writeValueAsString(param.getValue());
						jsonString = jsonString.replace("\"%s\"".formatted(key), listJson);
					} else if (param.getKey().toLowerCase().contains("date")) {
						String dateJson = """
								{ "$date": "%s" }
								""".formatted(param.getValue());
						jsonString = jsonString.replace("\"%s\"".formatted(key), dateJson);
					} else {
						jsonString = jsonString.replace(key, param.getValue().toString());
					}
				}
				Document queryDoc = Document.parse(jsonString);
				queryList.add(queryDoc);
			} catch (JsonProcessingException e) {
				log.error("Exception occured during buildQueryWithParams {}", e.getMessage());
			}
		}
		return queryList;
	}
	
	private Set<String> extractRequiredParams(String jsonString) {
	    Set<String> requiredParams = new HashSet<>();
	    Matcher matcher = Pattern.compile("\\{\\{(.*?)\\}\\}").matcher(jsonString);
	    while (matcher.find()) {
	        requiredParams.add(matcher.group(1));
	    }
	    return requiredParams;
	}
	
	private void validateParameters(String jsonString, Map<String, Object> parameters) throws BadRequestException {
		Set<String> requiredParams = extractRequiredParams(jsonString);
        Set<String> providedParams = parameters.keySet();
        Set<String> missingParams = new HashSet<>(requiredParams);
        missingParams.removeAll(providedParams);

        if (!missingParams.isEmpty()) {
            throw new BadRequestException(Report.class.getSimpleName(),"Missing required parameters " + missingParams.toString());
        }
	}

}
