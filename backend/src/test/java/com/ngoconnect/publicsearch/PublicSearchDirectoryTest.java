package com.ngoconnect.publicsearch;

import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.NgoService;
import com.ngoconnect.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PublicSearchDirectoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private NgoService ngoService;

    @Autowired
    private RestaurantService restaurantService;

    private Long ngoId;
    private Long restaurantId;

    @BeforeEach
    void setUp() {
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Green Earth Rep");
        ngoReq.setEmail("green.earth@ngoconnect.org");
        ngoReq.setPhone("9876543210");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Green Earth NGO");
        ngoReq.setCity("Lucknow");
        ngoReq.setAddress("Hazratganj");
        AuthResponse ngoResp = authService.register(ngoReq);
        ngoId = ngoService.getNgoProfile(ngoResp.getId()).getId();

        RegisterRequest restReq = new RegisterRequest();
        restReq.setName("Royal Spice Owner");
        restReq.setEmail("royal.spice@ngoconnect.org");
        restReq.setPhone("9876543211");
        restReq.setPassword("Password123!");
        restReq.setRole("RESTAURANT");
        restReq.setRestaurantName("Royal Spice Dining");
        restReq.setCity("Lucknow");
        restReq.setAddress("Gomti Nagar");
        AuthResponse restResp = authService.register(restReq);
        restaurantId = restaurantService.getRestaurantProfile(restResp.getId()).getId();
    }

    @Test
    @DisplayName("shouldAllowUnauthenticatedBrowsingOfPublicNgos")
    void shouldAllowUnauthenticatedBrowsingOfPublicNgos() throws Exception {
        mockMvc.perform(get("/api/v1/ngos/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ngoName").exists());
    }

    @Test
    @DisplayName("shouldAllowUnauthenticatedBrowsingOfPublicRestaurants")
    void shouldAllowUnauthenticatedBrowsingOfPublicRestaurants() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].restaurantName").exists());
    }

    @Test
    @DisplayName("shouldSearchNgosByCityCaseInsensitiveAndPartialMatch")
    void shouldSearchNgosByCityCaseInsensitiveAndPartialMatch() throws Exception {
        // Exact mixed case
        mockMvc.perform(get("/api/v1/ngos/public").param("city", "Lucknow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Lucknow"));

        // Lowercase
        mockMvc.perform(get("/api/v1/ngos/public").param("city", "lucknow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Lucknow"));

        // Partial match "Luck"
        mockMvc.perform(get("/api/v1/ngos/public").param("city", "Luck"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Lucknow"));
    }

    @Test
    @DisplayName("shouldGetPublicNgoById")
    void shouldGetPublicNgoById() throws Exception {
        mockMvc.perform(get("/api/v1/ngos/public/" + ngoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ngoId))
                .andExpect(jsonPath("$.ngoName").value("Green Earth NGO"));
    }

    @Test
    @DisplayName("shouldReturn404ForNonexistentNgoId")
    void shouldReturn404ForNonexistentNgoId() throws Exception {
        mockMvc.perform(get("/api/v1/ngos/public/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("shouldReturn404ForNonexistentRestaurantId")
    void shouldReturn404ForNonexistentRestaurantId() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/public/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
