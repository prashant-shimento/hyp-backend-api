package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.util.ReflectionTestUtils;

import com.hyp.entity.AddonGroup;
import com.hyp.repository.AddonGroupRepository;

@ExtendWith(MockitoExtension.class)
public class AddonGroupServiceTest {

	@Mock
	private AddonGroupRepository addonGroupRepository;
	@Mock
	private MongoTemplate mongoTemplate;
	@InjectMocks
	private BaseServiceImpl<AddonGroup, String> baseService = new BaseServiceImpl<AddonGroup, String>() {
	};
	AddonGroupService addonGroupService;
	List<AddonGroup> mockAddonGroups;
	AddonGroup addonGroup;

	@BeforeEach
	void setUp() {
		addonGroupService = new AddonGroupService();
		ReflectionTestUtils.setField(addonGroupService, "addonGroupRepository", addonGroupRepository);
		ReflectionTestUtils.setField(addonGroupService, "mongoTemplate", mongoTemplate);

		mockAddonGroups = new ArrayList<>();
		addonGroup = new AddonGroup();
		addonGroup.setId("123");
		addonGroup.setAddonGroupName("Toppings");
		addonGroup.setActive("true");
		addonGroup.setAddonGroupRank("1");
		addonGroup.setAddonGroupItems(Arrays.asList("Extra Cheese", "Pepperoni"));
		addonGroup.setAddonItemSelectionMax("2");
		addonGroup.setAddonItemSelectionMin("1");
		mockAddonGroups.add(addonGroup);
	}

	@Test
	void getAddonGroupsItemsById_Success() {
		String addonGroupId = "123";

		@SuppressWarnings("unchecked")
		AggregationResults<AddonGroup> mockResults = mock(AggregationResults.class);
		when(mockResults.getMappedResults()).thenReturn(mockAddonGroups);

		when(mongoTemplate.aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class)))
				.thenReturn(mockResults);

		List<AddonGroup> result = addonGroupService.getAddonGroupsAndItemsById(addonGroupId);

		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(addonGroupId);
		assertThat(result.get(0).getAddonGroupName()).isEqualTo("Toppings");

		verify(mongoTemplate).aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class));
	}

	@Test
	void getAddonGroupsItemsById_NotFound() {
		String addonGroupId = "iuytyui";

		@SuppressWarnings("unchecked")
		AggregationResults<AddonGroup> mockResults = mock(AggregationResults.class);
		when(mockResults.getMappedResults()).thenReturn(mockAddonGroups);

		when(mongoTemplate.aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class)))
				.thenReturn(mockResults);

		List<AddonGroup> result = addonGroupService.getAddonGroupsAndItemsById(addonGroupId);

		assertNotEquals(result, result.get(0).getId());

		verify(mongoTemplate).aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class));
	}

	@Test
	void getAddonGroupsItems_Success() {

		@SuppressWarnings("unchecked")
		AggregationResults<AddonGroup> mockResults = mock(AggregationResults.class);
		when(mockResults.getMappedResults()).thenReturn(mockAddonGroups);

		when(mongoTemplate.aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class)))
				.thenReturn(mockResults);

		List<AddonGroup> result = addonGroupService.getAllAddonGroupsAndItems();

		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.get(0).getAddonGroupName()).isEqualTo("Toppings");

		verify(mongoTemplate).aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class));
	}

	@Test
	void getAddonGroupsItems_NotFound() {
		String ExpectedAddonGroupId = "iuytyui";
		@SuppressWarnings("unchecked")
		AggregationResults<AddonGroup> mockResults = mock(AggregationResults.class);
		when(mockResults.getMappedResults()).thenReturn(mockAddonGroups);

		when(mongoTemplate.aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class)))
				.thenReturn(mockResults);

		String actualAddonGroupId = addonGroupService.getAllAddonGroupsAndItems().get(0).getId();

		assertNotEquals(ExpectedAddonGroupId, actualAddonGroupId);

		verify(mongoTemplate).aggregate(any(Aggregation.class), eq("addon_groups"), eq(AddonGroup.class));
	}

	// CRUD Operation
	@Test
    void findAddonGroupById_Success() {
        when(addonGroupRepository.findById("123")).thenReturn(Optional.of(addonGroup));

        AddonGroup result = baseService.findById("123");

        assertNotNull(result);
        assertThat(result.getId()).isEqualTo("123");
        verify(addonGroupRepository, times(1)).findById("123");
    }

	@Test
    void findAddonGroupById_NotFound() {
        when(addonGroupRepository.findById("456")).thenReturn(Optional.empty());

        AddonGroup result = baseService.findById("456");

        assertThat(result).isNull();
        verify(addonGroupRepository, times(1)).findById("456");
    }

	@Test
    void addAddonGroup_Success() {
        when(addonGroupRepository.save(addonGroup)).thenReturn(addonGroup);

        AddonGroup addedAddonGroup = baseService.save(addonGroup);

        assertNotNull(addedAddonGroup);
        assertThat(addedAddonGroup.getAddonGroupName()).isEqualTo(addonGroup.getAddonGroupName());
        assertThat(addedAddonGroup.getAddonGroupRank()).isEqualTo(addonGroup.getAddonGroupRank());
        verify(addonGroupRepository, times(1)).save(addonGroup);
    }

	@Test
    void findAllAddonGroups_Success() {
        when(addonGroupRepository.findAll()).thenReturn(mockAddonGroups);

        List<AddonGroup> addonGroups = baseService.findAll();

        assertNotNull(addonGroups);
        assertThat(addonGroups.size()).isEqualTo(1);
        assertThat(addonGroups.get(0).getId()).isEqualTo("123");
        verify(addonGroupRepository, times(1)).findAll();
    }

	@Test
    void updateAddonGroup_Success() {
        when(addonGroupRepository.save(addonGroup)).thenReturn(addonGroup);

        AddonGroup updatedAddonGroup = baseService.update(addonGroup);

        assertNotNull(updatedAddonGroup);
        assertThat(updatedAddonGroup.getAddonGroupName()).isEqualTo(addonGroup.getAddonGroupName());
        verify(addonGroupRepository, times(1)).save(addonGroup);
    }

	@Test
	void deleteAddonGroup_Success() {
		doNothing().when(addonGroupRepository).deleteById("123");

		baseService.deleteById("123");

		verify(addonGroupRepository, times(1)).deleteById("123");
	}
}
