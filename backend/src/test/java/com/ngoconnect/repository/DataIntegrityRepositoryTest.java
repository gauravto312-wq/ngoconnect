package com.ngoconnect.repository;

import com.ngoconnect.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class DataIntegrityRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    @DisplayName("shouldEnforceUniqueEmailConstraintInUserRepository")
    void shouldEnforceUniqueEmailConstraintInUserRepository() {
        Role role = roleRepository.save(Role.builder().name("CITIZEN").build());

        User user1 = User.builder()
                .name("Alice")
                .email("alice.unique@example.com")
                .phone("9876543210")
                .passwordHash("hashedpass")
                .role(role)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .name("Alice Copy")
                .email("alice.unique@example.com") // Duplicate Email
                .phone("9876543211")
                .passwordHash("hashedpass")
                .role(role)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
    }

    @Test
    @DisplayName("shouldPersistUserAndNgoRelationshipsCorrectly")
    void shouldPersistUserAndNgoRelationshipsCorrectly() {
        Role role = roleRepository.save(Role.builder().name("NGO").build());
        Location loc = locationRepository.save(Location.builder().address("Hazratganj").city("Lucknow").state("UP").build());

        User user = userRepository.save(User.builder()
                .name("NGO Owner")
                .email("ngo.owner@example.com")
                .phone("9876543210")
                .passwordHash("hashedpass")
                .role(role)
                .build());

        Ngo ngo = Ngo.builder()
                .user(user)
                .ngoName("Earth Rescue Foundation")
                .contactNumber("9876543210")
                .address("Hazratganj")
                .city("Lucknow")
                .state("UP")
                .location(loc)
                .verificationStatus("PENDING")
                .build();

        Ngo savedNgo = ngoRepository.saveAndFlush(ngo);

        assertNotNull(savedNgo.getId());
        assertEquals("PENDING", savedNgo.getVerificationStatus());
        assertEquals("Earth Rescue Foundation", savedNgo.getNgoName());
        assertEquals("Lucknow", savedNgo.getLocation().getCity());

        Optional<Ngo> foundByUserId = ngoRepository.findByUserId(user.getId());
        assertTrue(foundByUserId.isPresent());
        assertEquals(savedNgo.getId(), foundByUserId.get().getId());
    }

    @Test
    @DisplayName("shouldSearchNgosByCaseInsensitiveQueryAndCity")
    void shouldSearchNgosByCaseInsensitiveQueryAndCity() {
        Role role = roleRepository.save(Role.builder().name("NGO").build());

        User u1 = userRepository.save(User.builder().name("NGO 1").email("n1@example.com").phone("123").passwordHash("h").role(role).build());
        User u2 = userRepository.save(User.builder().name("NGO 2").email("n2@example.com").phone("456").passwordHash("h").role(role).build());

        ngoRepository.save(Ngo.builder().user(u1).ngoName("Green Earth").contactNumber("123").address("A").city("Lucknow").state("UP").verificationStatus("APPROVED").build());
        ngoRepository.save(Ngo.builder().user(u2).ngoName("Blue Ocean").contactNumber("456").address("B").city("Delhi").state("DL").verificationStatus("APPROVED").build());

        List<Ngo> lucknowNgos = ngoRepository.searchNgos("green", "lucknow");
        assertFalse(lucknowNgos.isEmpty());
        assertEquals("Green Earth", lucknowNgos.get(0).getNgoName());
    }
}
