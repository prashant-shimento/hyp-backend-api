package com.hyp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.entity.Address.Location;
import com.hyp.util.Utils;

@Service
public class AddressTranslation implements TranslationService<AddressDto, Address> {

	@Override
	public Address getEntity(AddressDto dto) {
		Address address = new Address();
		address.setId(Utils.genId());
		address.setAddress(dto.getAddress());
		address.setAddressType(dto.getAddressType());
		address.setLocation(new Location(dto.getLatitude(), dto.getLongitude()));
		return address;
	}

	@Override
	public AddressDto getDto(Address entity) {
		AddressDto dto = new AddressDto();
		dto.setAddress(entity.getAddress());
		dto.setAddressType(entity.getAddressType());

		Location location = entity.getLocation();
		if (location != null) {
			dto.setLatitude(entity.getLocation().getLatitude());
			dto.setLongitude(entity.getLocation().getLongitude());
		}

		return dto;
	}

	@Override
	public List<AddressDto> getDtoList(List<Address> entities) {
		List<AddressDto> dtoList = new ArrayList<>();
		for (Address entity : entities) {
			dtoList.add(getDto(entity));
		}
		return dtoList;
	}

	@Override
	public Address getPatchDto(Address existingEntity, AddressDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
