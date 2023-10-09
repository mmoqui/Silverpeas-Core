package org.silverpeas.core.process.check;

import org.silverpeas.core.SilverpeasException;

/**
 * Exception thrown when the validation of the execution of a process fails.
 * @author mmoquillon
 */
public class ProcessValidationException extends SilverpeasException {

  public ProcessValidationException(String message, String... parameters) {
    super(message, parameters);
  }

  public ProcessValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessValidationException(Throwable cause) {
    super(cause);
  }
}
  