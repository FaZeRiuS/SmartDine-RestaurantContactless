package com.example.CourseWork.integration.api;

import com.example.CourseWork.security.CurrentUserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
        "app.images.webp.enabled=false"
})
@Import({
        com.example.CourseWork.config.SecurityConfig.class,
        com.example.CourseWork.config.SecurityProperties.class,
        com.example.CourseWork.config.OAuth2ClientTestStubConfig.class
})
@SuppressWarnings("null")
class ImagesUploadApiIT {

    @Autowired MockMvc mockMvc;

    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
            org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
            org.springframework.security.oauth2.core.oidc.user.OidcUser> oidcUserService;
    @MockitoBean CurrentUserIdentity currentUserIdentity;

    @org.springframework.lang.NonNull
    private RequestPostProcessor login(String userId, String role) {
        doReturn(userId).when(currentUserIdentity).currentUserId();
        return oidcLogin()
                .idToken(token -> token.subject(userId))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void uploadImage_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/admin/dishes/upload-image")
                        .file(file)
                        .with(login("chef-1", "CHEF"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Файл порожній"));
    }

    @Test
    void uploadImage_validImage_returns200() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);

        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", baos.toByteArray());

        mockMvc.perform(multipart("/api/admin/dishes/upload-image")
                        .file(file)
                        .with(login("admin-1", "ADMINISTRATOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").isString());
    }

    @Test
    void uploadImage_deniesCustomer() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);

        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", baos.toByteArray());

        mockMvc.perform(multipart("/api/admin/dishes/upload-image")
                        .file(file)
                        .with(login("cust-1", "CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}

