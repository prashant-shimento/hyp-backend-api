package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Content;
import com.hyp.enums.ContentType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

@ExtendWith(MockitoExtension.class)
public class ContentServiceTest {

    @Mock
    private MongoRepository<Content, String> contentRepository;

    @InjectMocks
    private BaseServiceImpl<Content, String> contentService = new BaseServiceImpl<>() {};

    private Content content;

    @BeforeEach
    void setUp() {
        content = new Content();
        content.setId("123");
        content.setTitle("Sample Title");
        content.setImageUrl("http://example.com/sample-image.jpg");
        content.setDescription("This is a sample description for the content.");
        content.setType(ContentType.ADVERTISEMENT);
        content.setReferenceIds(new HashMap<String, List<String>>() {
            private static final long serialVersionUID = 1L;

            {
                put("referenceKey1", Arrays.asList("ref1", "ref2"));
                put("referenceKey2", Arrays.asList("ref3", "ref4"));
            }
        });
    }

    @Test
    void addContent_Success() {
        when(contentRepository.save(content)).thenReturn(content);

        Content addedContent = contentService.save(content);

        assertNotNull(addedContent);
        assertEquals(content.getTitle(), addedContent.getTitle());
        assertEquals(content.getImageUrl(), addedContent.getImageUrl());
        assertEquals(content.getDescription(), addedContent.getDescription());
        assertEquals(content.getType(), addedContent.getType());
        assertEquals(content.getReferenceIds(), addedContent.getReferenceIds());

        verify(contentRepository, times(1)).save(content);
    }

    @Test
    void findContentById_Success() {
        when(contentRepository.findById("123")).thenReturn(Optional.of(content));

        Content foundContent = contentService.findById("123");

        assertNotNull(foundContent);
        assertEquals("123", foundContent.getId());
        verify(contentRepository, times(1)).findById("123");
    }

    @Test
    void findContentById_NotFound() {
        when(contentRepository.findById("content67890")).thenReturn(Optional.empty());

        Content content = contentService.findById("content67890");

        assertNull(content);
        verify(contentRepository, times(1)).findById("content67890");
    }

    @Test
    void findAllContent_Success() {
        List<Content> mockContents = Arrays.asList(content, content);
        when(contentRepository.findAll()).thenReturn(mockContents);

        List<Content> contents = contentService.findAll();

        assertNotNull(contents);
        assertEquals(2, contents.size());
        verify(contentRepository, times(1)).findAll();
    }

    @Test
    void updateContent_Success() {
        when(contentRepository.save(content)).thenReturn(content);

        Content updatedContent = contentService.update(content);

        assertNotNull(updatedContent);
        assertEquals(content.getId(), updatedContent.getId());
        verify(contentRepository, times(1)).save(content);
    }

    @Test
    void deleteContent_Success() {
        doNothing().when(contentRepository).deleteById("123");
        contentService.deleteById("123");
        verify(contentRepository, times(1)).deleteById("123");
    }
}
