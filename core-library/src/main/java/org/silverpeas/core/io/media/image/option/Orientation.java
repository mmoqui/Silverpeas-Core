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
package org.silverpeas.core.io.media.image.option;

import java.util.stream.Stream;

import static org.silverpeas.kernel.util.StringUtil.isInteger;

/**
 * @author Yohann Chastagnier
 */
public enum Orientation {
  TOP_LEFT("TopLeft"),
  TOP_RIGHT("TopRight"),
  BOTTOM_RIGHT("BottomRight"),
  BOTTOM_LEFT("BottomLeft"),
  LEFT_TOP("LeftTop"),
  RIGHT_TOP("RightTop"),
  RIGHT_BOTTOM("RightBottom"),
  LEFT_BOTTOM("LeftBottom"),
  AUTO("auto");

  private final String toolName;

  Orientation(final String toolName) {
    this.toolName = toolName;
  }

  public static Orientation decode(final String orientation) {
    return Stream.of(values())
        .filter(v -> v.toolName.equalsIgnoreCase(orientation) ||
            (isInteger(orientation) && (v.ordinal() + 1) == Integer.parseInt(orientation)))
        .findFirst()
        .orElse(null);
  }

  public String getToolName() {
    return toolName;
  }
}
