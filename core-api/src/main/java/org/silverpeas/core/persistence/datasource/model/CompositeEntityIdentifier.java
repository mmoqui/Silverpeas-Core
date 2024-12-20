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
package org.silverpeas.core.persistence.datasource.model;

/**
 * A composite entity identifier is a unique identifier that is made up of several identification
 * values. Usually, these values are the unique identifier of one or more external entities.
 * @author ebonnet
 */
public interface CompositeEntityIdentifier extends ExternalEntityIdentifier {
  String COMPOSITE_SEPARATOR = ":";

  @Override
  default EntityIdentifier fromString(String id) {
    String[] values = id.split(COMPOSITE_SEPARATOR);
    return fromString(values);
  }

  /**
   * Sets the value of this identifier from the specified values that will be part of this
   * composite identifier.
   * @param values the identifier values from which this composite identifier will be built.
   * @return this entity identifier.
   */
  CompositeEntityIdentifier fromString(String... values);
}
