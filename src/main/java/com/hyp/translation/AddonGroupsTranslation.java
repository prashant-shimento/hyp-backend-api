package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.AddonGroupDTO;
import com.hyp.entity.AddonGroup;
import com.hyp.service.TranslationService;

@Service
public class AddonGroupsTranslation implements TranslationService<AddonGroupDTO, AddonGroup> {

	@Override
	public AddonGroup getEntity(AddonGroupDTO addongroupdto) {
		AddonGroup addonGroup = new AddonGroup();
		addonGroup.setId(addongroupdto.getAddongroupId());
		addonGroup.setAddonGroupRank(addongroupdto.getAddongroupRank());
		addonGroup.setActive(addongroupdto.getActive());
		addonGroup.setAddonGroupName(addongroupdto.getAddongroupName());
		addonGroup.setAddonItems(addongroupdto.getAddongroupitems());
		return addonGroup;
	}

	@Override
	public AddonGroupDTO getDto(AddonGroup addonGroup) {
		AddonGroupDTO addonGroupDTO = new AddonGroupDTO();

		addonGroupDTO.setAddongroupId(addonGroup.getId());
		addonGroupDTO.setAddongroupRank(addonGroup.getAddonGroupRank());
		addonGroupDTO.setActive(addonGroup.getActive());
		addonGroupDTO.setAddongroupName(addonGroup.getAddonGroupName());
		addonGroupDTO.setAddongroupitems(addonGroup.getAddonItems());
		return addonGroupDTO;

	}

	@Override
	public List<AddonGroupDTO> getDtoList(List<AddonGroup> addonGroupsList) {
		List<AddonGroupDTO> addonGroupDTOList = new ArrayList<>();

		for (AddonGroup addonGroup : addonGroupsList) {
			AddonGroupDTO addonGroupDTO = getDto(addonGroup);
			addonGroupDTOList.add(addonGroupDTO);
		}

		return addonGroupDTOList;
	}

	@Override
	public AddonGroup getPatchDto(AddonGroup existingEntity, AddonGroupDTO dto) {
		// TODO Auto-generated method stub
		return null;
	}
}
