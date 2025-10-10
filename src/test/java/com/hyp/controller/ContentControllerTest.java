package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.hyp.dto.ContentDto;
import com.hyp.entity.Content;
import com.hyp.enums.ContentType;
import com.hyp.response.Response;
import com.hyp.service.ContentService;
import com.hyp.translation.ContentTranslation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ContentControllerTest {

    @Mock
    private ContentService contentService;

    @Mock
    private ContentTranslation contentTranslation;

    @InjectMocks
    private BaseController<ContentDto, Content, String> contentController =
            new BaseController<ContentDto, Content, String>() {};

    private Content content;
    private ContentDto contentDto;

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

        contentDto = new ContentDto();
        contentDto.setId("123");
        contentDto.setTitle("Sample Title");
        contentDto.setImageUrl("http://example.com/sample-image.jpg");
        contentDto.setDescription("This is a sample description for the content.");
        contentDto.setType(ContentType.ADVERTISEMENT);
        contentDto.setReferenceIds(new HashMap<String, List<String>>() {
            private static final long serialVersionUID = 1L;

            {
                put("referenceKey1", Arrays.asList("ref1", "ref2"));
                put("referenceKey2", Arrays.asList("ref3", "ref4"));
            }
        });
    }

    @Test
    public void getContentById_Success() {
        when(contentService.findById("123")).thenReturn(content);
        ResponseEntity<Response> responseEntity = contentController.getById("123");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    @Test
    public void getContentById_NotFound() {
        when(contentService.findById("123")).thenReturn(null);
        ResponseEntity<Response> responseEntity = contentController.getById("123");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void createContent_Success() {
        when(contentTranslation.getEntity(contentDto)).thenReturn(content);
        when(contentService.save(content)).thenReturn(content);
        when(contentTranslation.getDto(content)).thenReturn(contentDto);

        ResponseEntity<Response> responseEntity = contentController.create(contentDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    @Test
    public void updateContent_Success() {
        when(contentService.findById("123")).thenReturn(content);
        doNothing().when(contentTranslation).updateEntityFromDto(contentDto, content);
        when(contentService.save(any(Content.class))).thenReturn(content);
        when(contentTranslation.getDto(content)).thenReturn(contentDto);

        ResponseEntity<Response> successResponseEntity = contentController.update("123", contentDto);

        assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(successResponseEntity.getBody()).isNotNull();
        assertThat(successResponseEntity.getBody().isError()).isFalse();
        assertThat(successResponseEntity.getBody().getMessage()).isEqualTo("success");
    }

    @Test
    public void updateContent_NotFound() {
        when(contentService.findById("123")).thenReturn(null);

        ResponseEntity<Response> responseEntity = contentController.update("123", contentDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void deleteContent_Success() {
        when(contentService.findById("123")).thenReturn(content);
        doNothing().when(contentService).deleteById("123");

        ResponseEntity<Response> successResponseEntity = contentController.delete("123");

        assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(successResponseEntity.getBody()).isNull();
    }

    @Test
    public void deleteContent_NotFound() {
        when(contentService.findById("123")).thenReturn(null);

        ResponseEntity<Response> successResponseEntity = contentController.delete("123");

        assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(successResponseEntity.getBody()).isNull();
    }

    @Test
    public void getAllContent_Success() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("type_eq", "ADVERTISEMENT");

        ResponseEntity<Response> responseEntity = contentController.getAll(queryParams);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    @Test
    public void getAllContent_BadRequest() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("title", "Sample Title");

        ResponseEntity<Response> responseEntity = contentController.getAll(queryParams);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
    }
}
