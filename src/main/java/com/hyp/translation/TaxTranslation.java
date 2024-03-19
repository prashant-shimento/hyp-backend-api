package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.TaxDto;
import com.hyp.entity.Tax;
import com.hyp.service.TranslationService;

@Service
public class TaxTranslation implements TranslationService<TaxDto, Tax> {

	@Override
	public Tax getEntity(TaxDto dto) {
		Tax taxEntity = new Tax();
		taxEntity.setId(dto.getTaxid());
		taxEntity.setTaxName(dto.getTaxname());
		taxEntity.setTaxTaxType(dto.getTax_taxtype());
		taxEntity.setTaxCoreOrTotal(dto.getTax_coreortotal());
		taxEntity.setRank(dto.getRank());
		taxEntity.setDescription(dto.getDescription());
		taxEntity.setActive(dto.getActive());
		taxEntity.setTax(dto.getTax());
		taxEntity.setConsiderInCoreAmount(dto.getConsider_in_core_amount());
		taxEntity.setTaxType(dto.getTaxtype());
		taxEntity.setTaxOrderType(dto.getTax_ordertype());
		return taxEntity;
	}

	@Override
	public TaxDto getDto(Tax entity) {
		TaxDto taxDto = new TaxDto();
		taxDto.setTaxid(entity.getId());
		taxDto.setTaxname(entity.getTaxName());
		taxDto.setTax(entity.getTax());
		taxDto.setTaxtype(entity.getTaxType());
		taxDto.setTax_ordertype(entity.getTaxOrderType());
		taxDto.setActive(entity.getActive());
		taxDto.setTax_coreortotal(entity.getTaxCoreOrTotal());
		taxDto.setTax_taxtype(entity.getTaxTaxType());
		taxDto.setRank(entity.getRank());
		taxDto.setConsider_in_core_amount(entity.getConsiderInCoreAmount());
		taxDto.setDescription(entity.getDescription());
		return taxDto;
	}

	@Override
	public List<TaxDto> getDtoList(List<Tax> entities) {
		List<TaxDto> taxDtoList = new ArrayList<>();
		for (Tax entity : entities) {
			TaxDto taxDto = new TaxDto();
			taxDto.setTaxid(entity.getId());
			taxDto.setTaxname(entity.getTaxName());
			taxDto.setTax(entity.getTax());
			taxDto.setTaxtype(entity.getTaxType());
			taxDto.setTax_ordertype(entity.getTaxOrderType());
			taxDto.setActive(entity.getActive());
			taxDto.setTax_coreortotal(entity.getTaxCoreOrTotal());
			taxDto.setTax_taxtype(entity.getTaxTaxType());
			taxDto.setRank(entity.getRank());
			taxDto.setConsider_in_core_amount(entity.getConsiderInCoreAmount());
			taxDto.setDescription(entity.getDescription());
			taxDtoList.add(taxDto);
		}
		return taxDtoList;
	}

	@Override
	public Tax getPatchDto(Tax existingEntity, TaxDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
