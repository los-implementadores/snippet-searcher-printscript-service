package org.snippetsearcher.printscript.shared;

/** A service this one depends on failed or is unreachable. */
public class DownstreamServiceException extends RuntimeException {

  public DownstreamServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
