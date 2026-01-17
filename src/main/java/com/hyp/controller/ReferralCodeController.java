package com.hyp.controller;

import com.hyp.dto.ReferralCodeDto;
import com.hyp.entity.ReferralCode;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.response.Response;
import com.hyp.service.ReferralCodeService;
import com.hyp.translation.ReferralCodeTranslation;
import jakarta.validation.Valid;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/referral")
public class ReferralCodeController extends BaseListController<ReferralCodeDto, ReferralCode, String> {

    @Autowired
    private ReferralCodeTranslation referralCodeTranslation;

    @Autowired
    private ReferralCodeService referralCodeService;

    /**
     * Creates a new referral code.
     */
    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody ReferralCodeDto referralCodeDto)
            throws BadRequestException {

        ReferralCodeDto savedDto = referralCodeService.create(referralCodeDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Response.builder()
                        .data(Collections.singletonList(savedDto))
                        .error(false)
                        .message("Referral code created successfully")
                        .build());
    }

    /**
     * Updates an existing referral code.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable String id, @RequestBody ReferralCodeDto referralCodeDto)
            throws EntityNotFoundException, BadRequestException {

        ReferralCodeDto updatedDto = referralCodeService.update(id, referralCodeDto);
        return ResponseEntity.ok(Response.builder()
                .data(Collections.singletonList(updatedDto))
                .error(false)
                .message("Referral code updated successfully")
                .build());
    }

    /**
     * Soft deletes a referral code.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable String id) throws EntityNotFoundException {
        referralCodeService.delete(id);
        return ResponseEntity.ok(Response.builder()
                .data(null)
                .error(false)
                .message("Referral code deleted successfully")
                .build());
    }
}
