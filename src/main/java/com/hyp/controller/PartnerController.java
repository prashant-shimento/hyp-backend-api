package com.hyp.controller;

import com.hyp.dto.PartnerDto;
import com.hyp.entity.Partner;
import com.hyp.response.Response;
import com.hyp.service.PartnerService;
import com.hyp.translation.PartnerTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/partner")
public class PartnerController extends BaseController<PartnerDto, Partner, String> {

    @Autowired
    public PartnerTranslation partnerTranslation;

    @Autowired
    PartnerService partnerService;

    @PostMapping(value = "/{partnerId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> uploadPartnerImages(
            @PathVariable String partnerId, MultipartHttpServletRequest multipartRequest) {

        MultiValueMap<String, MultipartFile> fileMap = multipartRequest.getMultiFileMap();

        try {
            partnerService.uploadAndSavePartnerImages(partnerId, fileMap);
            return ResponseEntity.ok(Response.builder()
                    .error(false)
                    .message("Images uploaded successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.builder()
                            .error(true)
                            .message("Image upload failed: " + e.getMessage())
                            .build());
        }
    }
}
