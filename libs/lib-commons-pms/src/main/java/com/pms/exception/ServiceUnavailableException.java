package com.pms.exception;

import java.io.Serial;

/**
 * Thrown when a required downstream service cannot be reached after all retries and the operation
 * cannot safely proceed (e.g. write validation). Maps to HTTP 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {

  @Serial private static final long serialVersionUID = -3209375309310837359L;

  public ServiceUnavailableException(String message) {
    super(message);
  }

  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
