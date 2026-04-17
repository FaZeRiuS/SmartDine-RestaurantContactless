package com.example.CourseWork.controller;

import com.example.CourseWork.service.DishService;
import com.example.CourseWork.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@WebMvcTest(DishImageController.class)
@SuppressWarnings("null")
class DishImageControllerTest extends BaseControllerTest {

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private ImageService imageService;

    @Test
    void uploadDishImage_ShouldSaveFileAndUpdateDish() throws Exception {
        // Arrange
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", imageBytes
        );

        when(imageService.processAndSaveImage(any())).thenReturn("/uploads/test.webp");

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/dishes/1/image")
                        .file(file)
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .with(csrf()))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").exists());

        verify(dishService).updateDishImage(eq(1), anyString());
    }

    private static final String EXPECTED_IMAGE_ERROR =
            "Не вдалося обробити або зберегти файл. Спробуйте інше зображення або зверніться до адміністратора.";

    @Test
    void uploadDishImage_WhenProcessingFails_ShouldReturnGenericErrorWithoutExceptionText() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", baos.toByteArray());

        when(imageService.processAndSaveImage(any()))
                .thenThrow(new IOException("secret-internal-path"));

        mockMvc.perform(multipart("/api/admin/dishes/1/image")
                        .file(file)
                        .with(withUser("admin-1", "ADMINISTRATOR"))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(EXPECTED_IMAGE_ERROR))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-internal-path"))));
    }

    @Test
    void uploadDishImage_ShouldDenyCustomer() throws Exception {
        // Arrange
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", imageBytes
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/dishes/1/image")
                        .file(file)
                        .with(withUser("cust-1", "CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
