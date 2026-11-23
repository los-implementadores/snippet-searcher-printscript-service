package org.snippetsearcher.template.shared;

/** The requested resource does not exist or the caller is not allowed to know it exists. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
