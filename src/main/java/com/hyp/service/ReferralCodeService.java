package com.hyp.service;

import com.hyp.dto.ReferralCodeDto;
import com.hyp.entity.ReferralCode;
import com.hyp.entity.Restaurant;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.ReferralCodeRepository;
import com.hyp.translation.ReferralCodeTranslation;
import com.hyp.util.CommonUtils;
import com.mongodb.DuplicateKeyException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralCodeService extends BaseServiceImpl<ReferralCode, String> {

    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralCodeTranslation referralCodeTranslation;

    public ReferralCodeDto create(ReferralCodeDto dto) throws BadRequestException {
        log.info("Creating referral code: referrerName={}", dto.getReferrerName());

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                if (dto.getCode() == null || dto.getCode().isBlank()) {
                    dto.setCode(generateReferralCode());
                }

                ReferralCode entity = referralCodeTranslation.getEntity(dto);
                ReferralCode saved = referralCodeRepository.save(entity);
                return referralCodeTranslation.getDto(saved);

            } catch (DuplicateKeyException ex) {
                log.warn("Referral code collision for code={}, retrying", dto.getCode());
                dto.setCode(null);
            }
        }

        throw new BadRequestException("ReferralCode", "Unable to generate unique referral code");
    }

    private String generateReferralCode() {
        return CommonUtils.genCode();
    }

    public ReferralCodeDto update(String id, ReferralCodeDto referralCodeDto)
            throws EntityNotFoundException, BadRequestException {
        log.info("Updating referral code: id={}", id);

        ReferralCode existingCode = Optional.ofNullable(findById(id))
                .orElseThrow(() -> new EntityNotFoundException(Restaurant.class.getSimpleName(), id));

        if (existingCode.isDeleted()) {
            throw new EntityNotFoundException("ReferralCode", id);
        }

        if (referralCodeDto.getCode() != null
                && !referralCodeDto.getCode().equals(existingCode.getCode())
                && referralCodeRepository.existsByCodeAndIsDeletedFalse(referralCodeDto.getCode())) {
            throw new BadRequestException("ReferralCode", "Referral code already exists");
        }

        referralCodeTranslation.updateEntityFromDto(referralCodeDto, existingCode);
        ReferralCode updatedCode = referralCodeRepository.save(existingCode);
        return referralCodeTranslation.getDto(updatedCode);
    }

    public void delete(String id) throws EntityNotFoundException {
        log.info("Deleting referral code: id={}", id);

        ReferralCode existingCode = Optional.ofNullable(findById(id))
                .orElseThrow(() -> new EntityNotFoundException(Restaurant.class.getSimpleName(), id));

        if (existingCode.isDeleted()) {
            throw new EntityNotFoundException("ReferralCode", id);
        }

        existingCode.setDeleted(true);
        existingCode.setActive(false);
        referralCodeRepository.save(existingCode);
    }
}
