package com.ngoconnect.verification;

import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.Ngo;
import com.ngoconnect.entity.NgoVerification;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.NgoVerificationRepository;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.security.JwtTokenProvider;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.CloudinaryService;
import com.ngoconnect.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NgoPrivateDocumentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private NgoVerificationRepository ngoVerificationRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CloudinaryService cloudinaryService;

    private User adminUser;
    private String adminToken;

    private User ngoUser1;
    private Ngo ngo1;
    private String ngoToken1;

    private User ngoUser2;
    private Ngo ngo2;
    private String ngoToken2;

    private User citizenUser;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));
        Role ngoRole = roleRepository.findByName("NGO")
                .orElseGet(() -> roleRepository.save(Role.builder().name("NGO").build()));
        Role citizenRole = roleRepository.findByName("CITIZEN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CITIZEN").build()));

        // Admin
        adminUser = userRepository.save(User.builder()
                .name("Security Admin")
                .email("admin.docsec@ngoconnect.org")
                .phone("9991112222")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(adminRole)
                .isActive(true)
                .build());
        com.ngoconnect.dto.AuthRequest adminLogin = new com.ngoconnect.dto.AuthRequest();
        adminLogin.setEmail("admin.docsec@ngoconnect.org");
        adminLogin.setPassword("AdminPass123!");
        adminToken = authService.login(adminLogin).getToken();

        // Citizen
        RegisterRequest citizenReg = new RegisterRequest();
        citizenReg.setName("Regular Citizen");
        citizenReg.setEmail("citizen.docsec@ngoconnect.org");
        citizenReg.setPhone("9993334444");
        citizenReg.setPassword("CitizenPass123!");
        citizenReg.setRole("CITIZEN");
        AuthResponse citizenResp = authService.register(citizenReg);
        citizenUser = userRepository.findById(citizenResp.getId()).orElseThrow();
        citizenToken = citizenResp.getToken();

        // NGO 1
        RegisterRequest reg1 = new RegisterRequest();
        reg1.setName("NGO Rep One");
        reg1.setEmail("ngo1.docsec@ngoconnect.org");
        reg1.setPhone("9871110001");
        reg1.setPassword("Password123!");
        reg1.setRole("NGO");
        reg1.setNgoName("Primary Relief NGO");
        reg1.setCause("Disaster Relief");
        reg1.setCity("Lucknow");
        reg1.setAddress("Aliganj Sector C");
        AuthResponse resp1 = authService.register(reg1);
        ngoUser1 = userRepository.findById(resp1.getId()).orElseThrow();
        ngo1 = ngoRepository.findByUserId(ngoUser1.getId()).orElseThrow();
        ngoToken1 = resp1.getToken();

        // NGO 2
        RegisterRequest reg2 = new RegisterRequest();
        reg2.setName("NGO Rep Two");
        reg2.setEmail("ngo2.docsec@ngoconnect.org");
        reg2.setPhone("9872220002");
        reg2.setPassword("Password123!");
        reg2.setRole("NGO");
        reg2.setNgoName("Secondary Shelter NGO");
        reg2.setCause("Shelter");
        reg2.setCity("Lucknow");
        reg2.setAddress("Indira Nagar");
        AuthResponse resp2 = authService.register(reg2);
        ngoUser2 = userRepository.findById(resp2.getId()).orElseThrow();
        ngo2 = ngoRepository.findByUserId(ngoUser2.getId()).orElseThrow();
        ngoToken2 = resp2.getToken();
    }

    @Test
    @DisplayName("shouldAllowNgoToUploadDocumentFileToLocalStorageWhenCloudinaryNotConfigured")
    void shouldAllowNgoToUploadDocumentFileToLocalStorageWhenCloudinaryNotConfigured() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "80g_certificate.pdf",
                "application/pdf",
                "%PDF-1.4 test certificate content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ngos/verification-documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/api/v1/ngos/verification-documents/private/doc_")));

        verify(cloudinaryService, never()).uploadPrivateDocument(any(), any());
    }

    @Test
    @DisplayName("shouldAllowNgoToUploadDocumentFileToCloudinaryWhenConfigured")
    void shouldAllowNgoToUploadDocumentFileToCloudinaryWhenConfigured() throws Exception {
        String expectedCloudinarySecureUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/doc_80g.pdf";
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadPrivateDocument(any(), eq("documents"))).thenReturn(expectedCloudinarySecureUrl);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "12a_cert.png",
                "image/png",
                "sample-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ngos/verification-documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedCloudinarySecureUrl));

        verify(cloudinaryService, times(1)).uploadPrivateDocument(any(), eq("documents"));
    }

    @Test
    @DisplayName("shouldRejectNonNgoRoleFromUploadingVerificationDocument")
    void shouldRejectNonNgoRoleFromUploadingVerificationDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                "application/pdf",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ngos/verification-documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectUnauthenticatedUploadOfVerificationDocument")
    void shouldRejectUnauthenticatedUploadOfVerificationDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                "application/pdf",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ngos/verification-documents/upload")
                        .file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectUnsupportedMimeTypeOrExtension")
    void shouldRejectUnsupportedMimeTypeOrExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/x-msdownload",
                "binary".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ngos/verification-documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file type")));
    }

    @Test
    @DisplayName("shouldAllowOwningNgoToViewDocumentUrlAndStreamLocalFile")
    void shouldAllowOwningNgoToViewDocumentUrlAndStreamLocalFile() throws Exception {
        // Create sample file on disk in private directory
        String filename = "doc_test_local_12a.pdf";
        Path privateFile = fileStorageService.getPrivateDocumentsDirectory().resolve(filename);
        Files.writeString(privateFile, "%PDF-1.4 Fake document content");

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("12A Registration")
                .documentNumber("12A-LKO-9988")
                .documentUrl("/api/v1/ngos/verification-documents/private/" + filename)
                .verificationStatus("PENDING")
                .build());

        // 1. Owning NGO gets view-url
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/v1/ngos/verification-documents/" + verif.getId() + "/file"));

        // 2. Owning NGO streams file
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fake document content")));
    }

    @Test
    @DisplayName("shouldAllowAdminToViewAnyNgoDocumentUrlAndStreamFile")
    void shouldAllowAdminToViewAnyNgoDocumentUrlAndStreamFile() throws Exception {
        String filename = "doc_test_admin_view.png";
        Path privateFile = fileStorageService.getPrivateDocumentsDirectory().resolve(filename);
        Files.write(privateFile, new byte[]{1, 2, 3, 4});

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("FCRA Registration")
                .documentNumber("FCRA-2024-001")
                .documentUrl("/api/v1/ngos/verification-documents/private/" + filename)
                .verificationStatus("PENDING")
                .build());

        // Admin views URL
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/v1/ngos/verification-documents/" + verif.getId() + "/file"));

        // Admin streams file
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    @DisplayName("shouldRejectOtherNgoFromAccessingPrivateDocumentFile")
    void shouldRejectOtherNgoFromAccessingPrivateDocumentFile() throws Exception {
        String filename = "doc_secret_ngo1.pdf";
        Path privateFile = fileStorageService.getPrivateDocumentsDirectory().resolve(filename);
        Files.writeString(privateFile, "Confidential NGO1 Document");

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("FCRA Registration")
                .documentNumber("FCRA-2024-SECRET")
                .documentUrl("/api/v1/ngos/verification-documents/private/" + filename)
                .verificationStatus("PENDING")
                .build());

        // NGO 2 attempts to get view-url of NGO 1 document -> 403 Forbidden
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url")
                        .header("Authorization", "Bearer " + ngoToken2))
                .andExpect(status().isForbidden());

        // NGO 2 attempts to download NGO 1 document -> 403 Forbidden
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectCitizenFromAccessingPrivateDocumentFile")
    void shouldRejectCitizenFromAccessingPrivateDocumentFile() throws Exception {
        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-SECRET-001")
                .documentUrl("/api/v1/ngos/verification-documents/private/secret.pdf")
                .verificationStatus("PENDING")
                .build());

        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectUnauthenticatedUserFromAccessingPrivateDocumentFile")
    void shouldRejectUnauthenticatedUserFromAccessingPrivateDocumentFile() throws Exception {
        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-SECRET-001")
                .documentUrl("/api/v1/ngos/verification-documents/private/secret.pdf")
                .verificationStatus("PENDING")
                .build());

        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectTokenQueryParameterAndRequireAuthorizationHeader")
    void shouldRejectTokenQueryParameterAndRequireAuthorizationHeader() throws Exception {
        String filename = "doc_token_param_test.pdf";
        Path privateFile = fileStorageService.getPrivateDocumentsDirectory().resolve(filename);
        Files.writeString(privateFile, "Token Param PDF Content");

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-PARAM-TEST")
                .documentUrl("/api/v1/ngos/verification-documents/private/" + filename)
                .verificationStatus("PENDING")
                .build());

        // Attempting access with ?token= query parameter alone MUST be rejected (403 Forbidden)
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .param("token", ngoToken1))
                .andExpect(status().isForbidden());

        // Access with proper Authorization: Bearer header succeeds (200 OK)
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().string("Token Param PDF Content"));
    }

    @Test
    @DisplayName("shouldRedirectToSignedCloudinaryUrlWhenAssetOnCloudinary")
    void shouldRedirectToSignedCloudinaryUrlWhenAssetOnCloudinary() throws Exception {
        String rawCloudinaryUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf";
        String expectedSignedUrl = "https://api.cloudinary.com/v1_1/test-cloud/image/download?api_key=123&expires_at=9999&public_id=doc&signature=abc";

        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.generateSignedDocumentUrl(rawCloudinaryUrl)).thenReturn(expectedSignedUrl);

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-CLOUD-01")
                .documentUrl(rawCloudinaryUrl)
                .verificationStatus("PENDING")
                .build());

        // View URL endpoint returns signed URL
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedSignedUrl));

        // File endpoint returns 302 redirect to signed URL
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedSignedUrl));
    }

    @Test
    @DisplayName("shouldFailClosedWith503WhenCloudinarySignedUrlFailsOnCloudinaryAsset")
    void shouldFailClosedWith503WhenCloudinarySignedUrlFailsOnCloudinaryAsset() throws Exception {
        String rawCloudinaryUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf";

        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.generateSignedDocumentUrl(rawCloudinaryUrl)).thenReturn(null);

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-FAIL-CLOSED")
                .documentUrl(rawCloudinaryUrl)
                .verificationStatus("PENDING")
                .build());

        // View-url falls back to backend streaming endpoint without returning raw URL
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/v1/ngos/verification-documents/" + verif.getId() + "/file"));

        // File endpoint fails closed with 503 instead of redirecting to raw/unsigned URL
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Secure document storage service is currently unavailable."));
    }

    @Test
    @DisplayName("shouldFailClosedWhenCloudinaryNotConfiguredForCloudinaryAsset")
    void shouldFailClosedWhenCloudinaryNotConfiguredForCloudinaryAsset() throws Exception {
        String rawCloudinaryUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf";

        when(cloudinaryService.isConfigured()).thenReturn(false);

        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-UNCONFIGURED")
                .documentUrl(rawCloudinaryUrl)
                .verificationStatus("PENDING")
                .build());

        // File endpoint fails closed with 503
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/file")
                        .header("Authorization", "Bearer " + ngoToken1))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Secure document storage service is currently unavailable."));
    }

    @Test
    @DisplayName("shouldRejectUnauthenticatedAccessToViewUrlEndpoint")
    void shouldRejectUnauthenticatedAccessToViewUrlEndpoint() throws Exception {
        NgoVerification verif = ngoVerificationRepository.save(NgoVerification.builder()
                .ngo(ngo1)
                .documentType("80G Certificate")
                .documentNumber("80G-UNAUTH-TEST")
                .documentUrl("https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf")
                .verificationStatus("PENDING")
                .build());

        // Unauthenticated access to view-url MUST be rejected
        mockMvc.perform(get("/api/v1/ngos/verification-documents/" + verif.getId() + "/view-url"))
                .andExpect(status().isForbidden());
    }
}
