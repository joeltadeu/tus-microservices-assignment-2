package com.pms.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign interceptor that propagates the inbound Authorization header to all outbound Feign calls
 * automatically.
 *
 * <p>When ms-appointment calls ms-doctor or ms-patient, this interceptor copies the JWT from the
 * current HTTP request context into the outbound Feign request. Neither DoctorClient nor
 * PatientClient needs any modification.
 *
 * <p>Register as a Spring bean in any microservice that uses Feign clients.
 */
@Slf4j
public class FeignJwtInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes == null) {
      log.warn("FeignJwtInterceptor: RequestAttributes is NULL for {}", template.url());
      return;
    }

    HttpServletRequest request = attributes.getRequest();
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    log.debug("FeignJwtInterceptor: authHeader present={} for {}",
            authHeader != null, template.url());

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      template.header(HttpHeaders.AUTHORIZATION, authHeader);
      log.debug("Propagating JWT to: {}", template.url());
    } else {
      log.warn("FeignJwtInterceptor: No Bearer token found in request to {}", template.url());
    }
  }
}
