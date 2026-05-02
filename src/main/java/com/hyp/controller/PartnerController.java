package com.hyp.controller;

import com.hyp.dto.PartnerDto;
import com.hyp.dto.PartnerPublicDto;
import com.hyp.entity.Partner;
import com.hyp.response.Response;
import com.hyp.service.PartnerService;
import com.hyp.translation.PartnerTranslation;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping(path = {"/api/v2/partner", "/api/v3/partner"})
public class PartnerController extends BaseController<PartnerDto, Partner, String> {

    @Autowired
    public PartnerTranslation partnerTranslation;

    @Autowired
    PartnerService partnerService;

    /**
     * Public endpoint — discovery and storefront resolution combined.
     * Without domain: returns all partners (discovery listing).
     * With domain:    returns that partner + its restaurants (storefront resolution).
     */
    @GetMapping("/storefront")
    public ResponseEntity<Response> discoverPartners(@RequestParam(required = false) String domain) {
        if (domain != null && !domain.isBlank()) {
            Partner partner = partnerService.findByDomain(domain);
            if (partner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Response(null, true, "No partner found for this domain."));
            }
            partnerService.populateReferences(partner);
            return ResponseEntity.ok(new Response(
                    Collections.singletonList(partnerTranslation.getPublicDto(partner)), false, "success"));
        }

        List<Partner> partners = partnerService.findAll();
        List<PartnerPublicDto> dtos =
                partners.stream().map(partnerTranslation::getPublicDto).collect(Collectors.toList());
        return ResponseEntity.ok(new Response(dtos, false, "success"));
    }

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
