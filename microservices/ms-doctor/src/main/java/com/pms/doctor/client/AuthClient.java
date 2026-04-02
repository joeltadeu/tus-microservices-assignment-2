package com.pms.doctor.client;

import com.pms.models.dto.auth.CreateUserRequest;
import com.pms.models.dto.auth.CreateUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client used by ms-doctor and ms-patient to create / disable auth accounts
 * in ms-auth after a domain record is saved or deleted.
 *
 * URL is resolved via Eureka using the service name "auth-service".
 * For local development override with client.auth.url in application-local.yml.
 */
@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/v1/users")
    CreateUserResponse createUser(@RequestBody CreateUserRequest request);

    @DeleteMapping("/v1/users/domain/{domainId}")
    void disableUser(@PathVariable Long domainId, @RequestParam String role);
}
