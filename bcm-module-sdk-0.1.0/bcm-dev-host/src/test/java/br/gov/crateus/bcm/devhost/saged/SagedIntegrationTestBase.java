package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
abstract class SagedIntegrationTestBase {

    static {
        try {
            java.net.URL url = new java.net.URL("http://localhost:2375/v1.41/info");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            java.io.InputStream is = status == 200 ? conn.getInputStream() : conn.getErrorStream();
            byte[] buf = new byte[256];
            int n = is.read(buf);
            System.err.println("[TC-DEBUG] Java HTTP GET localhost:2375/v1.41/info → " + status + " body=" + new String(buf, 0, n));
        } catch (Exception e) {
            System.err.println("[TC-DEBUG] Java HTTP test FAILED: " + e);
        }
    }

    static final UUID TEST_DEPT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("bcm_test")
            .withUsername("bcm")
            .withPassword("bcm");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void truncateAllSagedTables() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                saged.bot_processed_messages,
                saged.telegram_contacts,
                saged.telegram_requester_authorizations,
                saged.telegram_demand_requesters,
                saged.telegram_technicians,
                saged.telegram_sessions,
                saged.telegram_link_codes,
                saged.user_specialties,
                saged.demand_views,
                saged.system_notifications,
                saged.demand_history,
                saged.demands,
                saged.assets,
                saged.specialties
            CASCADE
            """);
    }

    protected static JwtRequestPostProcessor adminJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_ADMIN_GERAL"))
                .jwt(t -> t.subject(TEST_USER_ID.toString())
                           .claim("org_unit_id", TEST_DEPT_ID.toString()));
    }

    protected static JwtRequestPostProcessor tecnicoJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO"))
                .jwt(t -> t.subject(TEST_USER_ID.toString())
                           .claim("org_unit_id", TEST_DEPT_ID.toString())
                           .claim("specialty_codes", java.util.List.of("HW")));
    }

    protected static JwtRequestPostProcessor adminSetorJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_ADMIN_SETOR"))
                .jwt(t -> t.subject(TEST_USER_ID.toString())
                           .claim("org_unit_id", TEST_DEPT_ID.toString()));
    }

    protected static JwtRequestPostProcessor lidJwt() {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO_LIDER"))
                .jwt(t -> t.subject(TEST_USER_ID.toString())
                           .claim("org_unit_id", TEST_DEPT_ID.toString())
                           .claim("specialty_codes", java.util.List.of("HW")));
    }
}
