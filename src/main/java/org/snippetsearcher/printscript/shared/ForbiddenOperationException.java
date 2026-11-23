package org.snippetsearcher.printscript.shared;

/** The caller can see the resource but is not allowed to do this with it. */
public class ForbiddenOperationException extends RuntimeException {

  public ForbiddenOperationException(String message) {
    super(message);
  }
}
