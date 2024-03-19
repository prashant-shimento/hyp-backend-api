package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;
import com.hyp.service.TranslationService;
import com.hyp.util.CommonUtils;

@Service
public class CustomerTranslation implements TranslationService<CustomerDto, Customer> {

	@Override
	public Customer getEntity(CustomerDto dto) {
		Customer customer = new Customer();
		dto.setId(CommonUtils.genId());
		BeanUtils.copyProperties(dto, customer);
		return customer;
	}

	@Override
	public CustomerDto getDto(Customer entity) {
		CustomerDto dto = new CustomerDto();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}

	@Override
	public List<CustomerDto> getDtoList(List<Customer> entities) {

		List<CustomerDto> dtoList = new ArrayList<>();

		for (Customer customer : entities) {
			CustomerDto dto = new CustomerDto();
			dto.setId(customer.getId());
			dto.setName(customer.getName());
			dto.setMobile(customer.getMobile());
			dto.setEmail(customer.getEmail());
			dto.setAddress_id(customer.getAddress_id());
			dtoList.add(dto);
		}

		return dtoList;
	}

	@Override
	public Customer getPatchDto(Customer existingEntity, CustomerDto dto) {
		if (existingEntity == null || dto == null) {
			return null;
		}

		Customer patchedEntity = new Customer();
		patchedEntity.setName(existingEntity.getName());
		patchedEntity.setMobile(existingEntity.getMobile());
		patchedEntity.setEmail(existingEntity.getEmail());
		patchedEntity.setAddress_id(existingEntity.getAddress_id());

		// here we need to check the id also but leave it for now....
		if (dto.getName() != null) {
			patchedEntity.setName(dto.getName());
		}
		if (dto.getMobile() != null) {
			patchedEntity.setMobile(dto.getMobile());
		}
		if (dto.getEmail() != null) {
			patchedEntity.setEmail(dto.getEmail());
		}
		if (dto.getAddress_id() != null) {
			patchedEntity.setAddress_id(dto.getAddress_id());
		}

		return patchedEntity;
	}

}
