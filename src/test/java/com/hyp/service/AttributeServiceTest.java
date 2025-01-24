package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Attribute;

@ExtendWith(MockitoExtension.class)
public class AttributeServiceTest {

	@Mock
	private MongoRepository<Attribute, String> attributeRepository;

	@InjectMocks
	private BaseServiceImpl<Attribute, String> attributeService = new BaseServiceImpl<>() {
	};

	private Attribute attribute;

	@BeforeEach
	void setUp() {
		attribute = new Attribute();
		attribute.setId("123");
		attribute.setAttribute("Attribute");
		attribute.setActive("True");
	}

	@Test
    void addAttribute_Success() {
        when(attributeRepository.save(attribute)).thenReturn(attribute);

        Attribute addedAttribute = attributeService.save(attribute);

        assertNotNull(addedAttribute);
        assertEquals(attribute.getAttribute(), addedAttribute.getAttribute());
        assertEquals(attribute.getActive(), addedAttribute.getActive());

        verify(attributeRepository, times(1)).save(attribute);
    }

	@Test
    void attributeById_Success() {
        when(attributeRepository.findById("123")).thenReturn(java.util.Optional.of(attribute));

        Attribute foundAttribute = attributeService.findById("123");

        assertNotNull(foundAttribute);
        assertEquals("123", foundAttribute.getId());
        verify(attributeRepository, times(1)).findById("123");
    }

	@Test
    void attributeById_NotFound() {
        when(attributeRepository.findById("456")).thenReturn(java.util.Optional.empty());

        Attribute foundAttribute = attributeService.findById("456");

        assertNull(foundAttribute);
        verify(attributeRepository, times(1)).findById("456");
    }

	@Test
	void findAllAttribute_Success() {
		java.util.List<Attribute> mockAttributes = java.util.Arrays.asList(attribute, attribute);
		when(attributeRepository.findAll()).thenReturn(mockAttributes);

		java.util.List<Attribute> attributes = attributeService.findAll();

		assertNotNull(attributes);
		assertEquals(2, attributes.size());
		verify(attributeRepository, times(1)).findAll();
	}

	@Test
    void updateAttribute_Success() {
        when(attributeRepository.save(attribute)).thenReturn(attribute);

        Attribute updatedAttribute = attributeService.update(attribute);

        assertNotNull(updatedAttribute);
        assertEquals(attribute.getId(), updatedAttribute.getId());
        verify(attributeRepository, times(1)).save(attribute);
    }

	@Test
	void deleteAttribute_Success() {
		doNothing().when(attributeRepository).deleteById("123");
		attributeService.deleteById("123");
		verify(attributeRepository, times(1)).deleteById("123");
	}
}