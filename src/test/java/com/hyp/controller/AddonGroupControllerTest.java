package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hyp.entity.AddonGroup;
import com.hyp.response.Response;
import com.hyp.service.AddonGroupService;
import com.hyp.translation.AddonGroupsTranslation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AddonGroupControllerTest {

    @Mock
    public AddonGroupsTranslation addonGroupTranslation;

    @Mock
    public AddonGroupService addonGroupService;

    AddonGroupController addonGroupController;

    List<AddonGroup> mockAddonGroups;
    AddonGroup addonGroup;

    @BeforeEach
    void setUp() {
        addonGroupController = new AddonGroupController();
        ReflectionTestUtils.setField(addonGroupController, "addonGroupTranslation", addonGroupTranslation);
        ReflectionTestUtils.setField(addonGroupController, "addonGroupService", addonGroupService);

        addonGroup = new AddonGroup();
        addonGroup.setId("123");
        addonGroup.setAddonGroupName("Toppings");
        addonGroup.setActive("true");
        addonGroup.setAddonGroupRank("1");
        addonGroup.setAddonGroupItems(Arrays.asList("Extra Cheese", "Pepperoni"));
        addonGroup.setAddonItemSelectionMax("2");
        addonGroup.setAddonItemSelectionMin("1");
        mockAddonGroups = new ArrayList<>();
        mockAddonGroups.add(addonGroup);
    }

    @Test
    public void getAddonGroupsAndItems_Success() {
        when(addonGroupService.getAllAddonGroupsAndItems()).thenReturn(mockAddonGroups);

        ResponseEntity<Response> responseEntity = addonGroupController.getAddonGroupsAndItems();

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.isError()).isFalse();
    }

    @Test
    public void getAddonGroupsAndItemsById_Success() {
        String addonGroupId = "123";
        when(addonGroupService.getAddonGroupsAndItemsById(addonGroupId)).thenReturn(mockAddonGroups);
        when(addonGroupService.isExistsById(addonGroupId)).thenReturn(true);

        ResponseEntity<Response> responseEntity = addonGroupController.getAddonGroupsAndItemsById(addonGroupId);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.isError()).isFalse();
    }

    @Test
    public void getAddonGroupsAndItemsById_NotFound() {
        String addonGroupId = "12322";
        when(addonGroupService.isExistsById(addonGroupId)).thenReturn(false);
        ResponseEntity<Response> responseEntity = addonGroupController.getAddonGroupsAndItemsById(addonGroupId);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.getMessage()).isEqualTo("AddonGroup not found " + addonGroupId);
        assertThat(response.getData()).isNull();
    }
}
