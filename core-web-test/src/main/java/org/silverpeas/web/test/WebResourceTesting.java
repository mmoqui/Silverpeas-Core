/*
 * Copyright (C) 2000 - 2024 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.silverpeas.web.test;

/**
 * Unit tests on the services provided by a web resource in Silverpeas.
 * This interface defines the operations a unit test about web resources should implement.
 */
public interface WebResourceTesting {

  /**
   * Gets the URI of a valid and existing web resource backed by a REST web service.
   * @return the URI of a valid web resource.
   */
  String aResourceURI();

  /**
   * Gets the URI of an invalid or an unexisting web resource. It should be an invalid URI (for
   * example a resource refered in an unexisting component instance), and not necessary an invalid
   * resource in a correct URI.
   * @return the URI of an invalid or unexisting web resource.
   */
  String anUnexistingResourceURI();

  /**
   * Gets a resource instance to use in tests.
   * @param <T> the type of the resource.
   * @return a resource.
   */
  <T> T aResource();

  /**
   * Gets the valid API token value to use in tests.
   * To be authorize to consume the REST API in the tests, the user must pass its API token
   * through a bearer authorization scheme.
   * @return a valid API user token key.
   */
  String getAPITokenValue();


  /**
   * Gets the class of the web entities handled by the REST web service to test.
   * @return the class of the web entities.
   */
  Class<?> getWebEntityClass();

}