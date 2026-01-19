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
package org.silverpeas.core.i18n;

import org.silverpeas.core.util.ServiceProvider;

import java.util.Set;

/**
 * This interface defines all the i18n related stuff as it is configured in Silverpeas: the default
 * language, all the languages supported in the current Silverpeas, and so on. The languages are the
 * ones that are supported by Silverpeas for the content of the resources managed in Silverpeas.
 * They aren't related to the user languages.
 *
 * @author mmoquillon
 */
public interface I18n {

  /**
   * Gets an instance of {@link I18n}.
   *
   * @return an instance of {@link I18n}
   */
  static I18n get() {
    return ServiceProvider.getService(I18n.class);
  }

  /**
   * Gets the default language of the platform when no one is explicitly specified.
   *
   * @return the ISO 639-1 code of the default language.
   */
  String getDefaultLanguage();

  /**
   * Gets the languages that are supported by the platform and from which users can choose their
   * preferred one.
   *
   * @return a set of ISO 639-1 codes of languages.
   */
  Set<String> getSupportedLanguages();

  /**
   * Checks the specified language is supported by the platform otherwise, the default supported one
   * is returned.
   *
   * @param language the ISO 639-1 code of a language.
   * @return either the ISO 639-1 of the specified language if it is supported by Silverpeas or,
   * otherwise, of the default language.
   */
  String checkLanguage(String language);

  /**
   * Checks the specified language is the default one. It doesn't take care of the case.
   *
   * @param language the ISO 639-1 code of a language.
   * @return true if the specified language is the default one, false otherwise.
   */
  boolean isDefaultLanguage(String language);

  /**
   * Is the i18n content support enabled?
   *
   * @return true if the i18n support is enabled for the resources' content in the current
   * Silverpeas.
   */
  boolean isEnabled();
}
