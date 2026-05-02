package com.hyp.controller;

import com.hyp.dto.FeedbackDto;
import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.response.Response;
import com.hyp.service.FeedbackService;
import com.hyp.service.PartnerService;
import com.hyp.translation.FeedbackTranslation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/feedback", "/api/v3/feedback"})
public class FeedbackController extends BaseController<FeedbackDto, Feedback, String> {

    @Autowired
    public FeedbackTranslation feedbackTranslation;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PartnerService partnerService;

    @GetMapping("/consume")
    public ResponseEntity<Response> consumeFeedback() {
        Response response;
        try {
            List<Partner> partners = partnerService.findByPartnerType(PartnerType.NOTIFICATION);

            if (partners.isEmpty()) {
                response = new Response(null, true, "No partners found for type NOTIFICATION");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            for (Partner partner : partners) {
                String chatIoUrl = feedbackService.constructChatIoUrl(partner);
                feedbackService.getDataFromChatIO(chatIoUrl, partner);
            }

            response = new Response(null, false, "Feedback Successfully Consumed from ChatIO");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to consume feedback from ChatIO", e);
            response = new Response(null, true, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
