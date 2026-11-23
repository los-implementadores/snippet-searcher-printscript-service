package org.snippetsearcher.template.shared;

/** The request is well formed but its content doesn't make sense for this operation. */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
