package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.AddonItem;

@ExtendWith(MockitoExtension.class)
public class AddonItemServiceTest {
	@Mock
	private MongoRepository<AddonItem, String> addonItemRepository;

	@InjectMocks
	private BaseServiceImpl<AddonItem, String> addonItemService = new BaseServiceImpl<>() {
	};

	private AddonItem addonItem;

	@BeforeEach
	void setUp() {
		addonItem = new AddonItem();
		addonItem.setId("56456");
		addonItem.setAddonItemName("Extra Cheese");
		addonItem.setAddonItemPrice("50");
		addonItem.setActive("true");
		addonItem.setAttributes("Dairy");
		addonItem.setAddonItemRank("1");
		addonItem.setAddonItemSelectionMin("1");
		addonItem.setAddonItemSelectionMax("1");
		addonItem.setAutoTurnOnTime(LocalDateTime.now());
	}

	@Test
    void addAddonItem_Success() {
        when(addonItemRepository.save(addonItem)).thenReturn(addonItem);

        AddonItem addedAddonItem = addonItemService.save(addonItem);

        assertNotNull(addedAddonItem);
        assertEquals(addonItem.getId(), addedAddonItem.getId());
        assertEquals(addonItem.getAddonItemName(), addedAddonItem.getAddonItemName());
        assertEquals(addonItem.getAddonItemPrice(), addedAddonItem.getAddonItemPrice());

        verify(addonItemRepository, times(1)).save(addonItem);
    }

	@Test
    void findAddonItemById_Success() {
        when(addonItemRepository.findById("56456")).thenReturn(Optional.of(addonItem));

        AddonItem foundAddonItem = addonItemService.findById("56456");

        assertNotNull(foundAddonItem);
        assertEquals("56456", foundAddonItem.getId());
        assertEquals("Extra Cheese", foundAddonItem.getAddonItemName());
        assertEquals("50", foundAddonItem.getAddonItemPrice());

        verify(addonItemRepository, times(1)).findById("56456");
    }

	@Test
    void findAddonItemById_NotFound() {
        when(addonItemRepository.findById("99999")).thenReturn(Optional.empty());

        AddonItem addonItem = addonItemService.findById("99999");

        assertNull(addonItem);
        verify(addonItemRepository, times(1)).findById("99999");
    }

	@Test
	void findAllAddonItems_Success() {
		List<AddonItem> mockAddonItems = Arrays.asList(addonItem, addonItem);
		when(addonItemRepository.findAll()).thenReturn(mockAddonItems);

		List<AddonItem> addonItems = addonItemService.findAll();

		assertNotNull(addonItems);
		assertEquals(2, addonItems.size());
		verify(addonItemRepository, times(1)).findAll();
	}

	@Test
    void updateAddonItem_Success() {
        when(addonItemRepository.save(addonItem)).thenReturn(addonItem);

        AddonItem updatedAddonItem = addonItemService.save(addonItem);

        assertNotNull(updatedAddonItem);
        assertEquals(addonItem.getId(), updatedAddonItem.getId());
        assertEquals(addonItem.getAddonItemName(), updatedAddonItem.getAddonItemName());
        verify(addonItemRepository, times(1)).save(addonItem);
    }

	@Test
	void deleteAddonItem_Success() {
		doNothing().when(addonItemRepository).deleteById("56456");

		addonItemService.deleteById("56456");

		verify(addonItemRepository, times(1)).deleteById("56456");
	}
}
