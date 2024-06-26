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
package org.silverpeas.core.admin.space.quota;

import org.silverpeas.core.admin.quota.constant.QuotaType;

import org.silverpeas.core.admin.space.SpaceInst;

/**
 * @author Yohann Chastagnier
 */
public class ComponentSpaceQuotaKey extends AbstractSpaceQuotaKey {

  /**
   * Initializing a quota key from a given space
   * @param space
   * @return
   */
  public static ComponentSpaceQuotaKey from(final SpaceInst space) {
    return new ComponentSpaceQuotaKey(space);
  }

  /**
   * Builds the componant space quota key from a given SpaceInst
   * @param space
   */
  private ComponentSpaceQuotaKey(final SpaceInst space) {
    super(space);
  }

  /*
   * (non-Javadoc)
   * @see QuotaKey#getQuotaType()
   */
  @Override
  public QuotaType getQuotaType() {
    return QuotaType.COMPONENTS_IN_SPACE;
  }
}
