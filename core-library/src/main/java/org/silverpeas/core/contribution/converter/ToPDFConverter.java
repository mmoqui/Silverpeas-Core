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
package org.silverpeas.core.contribution.converter;

/**
 * A converter of following listed documents into PDF format :
 * - ODT
 * - DOC
 * The converter is managed by the IoC container and can be retrieving under the name
 * 'toPDFConverter'.
 */
public interface ToPDFConverter extends DocumentFormatConversion {

  /**
   * Is the specified document in the format on which the converter works?
   * @param fileName the fileName or path to check its format is supported.
   * @return true if the format of the document is supported by this converter, false otherwise.
   */
  boolean isDocumentSupported(final String fileName);
}
