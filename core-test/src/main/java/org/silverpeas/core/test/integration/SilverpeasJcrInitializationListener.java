package org.silverpeas.core.test.integration;

import org.silverpeas.core.initialization.Initialization;
import org.silverpeas.core.initialization.SilverpeasServiceInitialization;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.function.Predicate;

/**
 * In order to initialize the JCR that can be used in integration tests, this listener MUST be
 * defined into the web.xml of the archive built of an integration test with arquillian + Wildfly.
 * <p>
 * At server starting, when the context of the integration is initialized, each implementation of
 * {@link Initialization} interface that the class name contains the keyword
 * "SilverpeasJcrSchemaRegistering" is performed.
 * </p>
 * <p>
 * By default, this listener is added onto the war archive used in the integration tests by the
 * {@link org.silverpeas.core.test.BasicWarBuilder} object.
 * </p>
 * @author silveryocha
 */
public class SilverpeasJcrInitializationListener implements ServletContextListener {

  private static final Predicate<Initialization> FILTER =
      i -> i.getClass().getSimpleName().contains("SilverpeasJcrSchemaRegistering");

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    SilverpeasServiceInitialization.start(FILTER);
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    SilverpeasServiceInitialization.stop(FILTER);
  }
}