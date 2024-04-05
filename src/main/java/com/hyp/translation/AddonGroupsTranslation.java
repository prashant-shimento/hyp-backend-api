package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.AddonGroupDto;
import com.hyp.entity.AddonGroup;
import com.hyp.service.TranslationService;

@Service
public class AddonGroupsTranslation implements TranslationService<AddonGroupDto, AddonGroup> {

	@Override
	public AddonGroup getEntity(AddonGroupDto addongroupdto) {
		AddonGroup addonGroup = new AddonGroup();
		addonGroup.setId(addongroupdto.getId());
		addonGroup.setAddonGroupRank(addongroupdto.getAddonGroupRank());
		addonGroup.setActive(addongroupdto.getActive());
		addonGroup.setAddonGroupName(addongroupdto.getAddonGroupName());
		addonGroup.setAddonGroupItems(addongroupdto.getAddonGroupItems());
		return addonGroup;
	}

	@Override
	public AddonGroupDto getDto(AddonGroup addonGroup) {
		AddonGroupDto addonGroupDTO = new AddonGroupDto();

		addonGroupDTO.setId(addonGroup.getId());
		addonGroupDTO.setAddonGroupRank(addonGroup.getAddonGroupRank());
		addonGroupDTO.setActive(addonGroup.getActive());
		addonGroupDTO.setAddonGroupName(addonGroup.getAddonGroupName());
		addonGroupDTO.setAddonGroupItems(addonGroup.getAddonGroupItems());
		return addonGroupDTO;

	}

	@Override
	public List<AddonGroupDto> getDtoList(List<AddonGroup> addonGroupsList) {
		List<AddonGroupDto> addonGroupDTOList = new ArrayList<>();

		for (AddonGroup addonGroup : addonGroupsList) {
			AddonGroupDto addonGroupDTO = getDto(addonGroup);
			addonGroupDTOList.add(addonGroupDTO);
		}

		return addonGroupDTOList;
	}

	@Override
	public AddonGroup getPatchDto(AddonGroup existingEntity, AddonGroupDto dto) {
		// TODO Auto-generated method stub
		return null;
	}
}
