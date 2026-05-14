package com.Apex.Apex_Gestordemo;

import Repository.LicenseActivationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "apex.license.catalog=TEST-WEB|web-client|1|30"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LicenseControllerSecurityTests {

    @LocalServerPort
    private int port;

    @Autowired
    private LicenseActivationRepository licenseActivationRepository;

    @BeforeEach
    void cleanLicenses() {
        licenseActivationRepository.deleteAll();
    }

    @Test
    void validateLicenseEndpointIsPublicForApplicationBootstrap() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/licenses/validate"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "licenseKey": "TEST-WEB",
                          "deviceFingerprint": "ci-web-device",
                          "deviceLabel": "CI Web",
                          "platform": "web",
                          "appVersion": "ci",
                          "appId": "web-client"
                        }
                        """))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"valid\":true");
        assertThat(response.body()).contains("\"appId\":\"web-client\"");
    }
}
