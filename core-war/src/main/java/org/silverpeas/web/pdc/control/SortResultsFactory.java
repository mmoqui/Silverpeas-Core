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
package org.silverpeas.web.pdc.control;

import org.silverpeas.kernel.bundle.ResourceLocator;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.kernel.bundle.SettingBundle;
import org.silverpeas.kernel.util.StringUtil;
import org.silverpeas.kernel.logging.SilverLogger;

/**
 * @author david derigent
 */
public class SortResultsFactory {

  /**
   *
   */
  private SortResultsFactory() {
  }

  /**
   * returns an implementation of SortResults interface according to the given keyword
   * @param implementor keyword corresponding to a key in searchEngineSettings.properties. this key
   * allows to gets a class name corresponding to a SortResults implementation
   * @return a SortResults implementation
   */
  public static SortResults getSortResults(String implementor) {
    SettingBundle settings =
        ResourceLocator.getSettingBundle("org.silverpeas.index.search.searchEngineSettings");
    String qualifier = settings.getString(implementor, "defaultSortResults");
    if (StringUtil.isDefined(qualifier)) {
      try {
        return (SortResults) ServiceProvider.getService(qualifier);
      } catch (Exception e) {
        SilverLogger.getLogger(SortResultsFactory.class).error("Sort result error", e);
      }
    }
    return ServiceProvider.getService("defaultSortResults");
  }
}
