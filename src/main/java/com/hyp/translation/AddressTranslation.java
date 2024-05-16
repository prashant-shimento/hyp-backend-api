package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.entity.Address.Location;
import com.hyp.service.TranslationService;
import com.hyp.util.CommonUtils;

@Service
public class AddressTranslation implements TranslationService<AddressDto, Address> {

	@Override
	public Address getEntity(AddressDto dto) {
		Address address = new Address();
		address.setId(dto.getId() == null ? CommonUtils.genId() : dto.getId());
		address.setAddressType(dto.getAddressType());
		address.setAddressOne(dto.getAddressOne());
		address.setAddressTwo(dto.getAddressTwo());
		address.setLandmark(dto.getLandmark());
		address.setCity(dto.getCity());
		address.setState(dto.getState());
		address.setCountry(dto.getCountry());
		address.setPincode(dto.getPincode());
		address.setLocation(new Location(dto.getLatitude(), dto.getLongitude()));
		return address;
	}

	@Override
	public AddressDto getDto(Address entity) {
		AddressDto dto = new AddressDto();
		dto.setAddressType(entity.getAddressType());
		dto.setAddressOne(entity.getAddressOne());
		dto.setAddressTwo(entity.getAddressTwo());
		dto.setLandmark(entity.getLandmark());
		dto.setCity(entity.getCity());
		dto.setState(entity.getState());
		dto.setCountry(entity.getCountry());
		dto.setPincode(entity.getPincode());

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
