/*
 * Copyright (C) 2000 - 2022 Silverpeas
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
package org.silverpeas.core.contribution.content.form.fileitem;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.silverpeas.core.util.Charsets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * File item created manually, without being retrieved from an HTTP request. Used to update an
 * imported publication's form.
 *
 * @author Antoine HEDIN
 */
public class InternalFileItem implements FileItem<InternalFileItem> {

  private static final long serialVersionUID = 754043464419634444L;

  private String fieldName;
  private String value;

  public InternalFileItem(String fieldName, String value) {
    setFieldName(fieldName);
    setValue(value);
  }

  @Override
  public InternalFileItem setFieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String getString() {
    return value;
  }

  @Override
  public String getString(Charset charset) throws IOException {
    return new String(value.getBytes(charset), charset);
  }

  /**
   * Does nothing.
   *
   * @param formField {@code true} if the instance represents a simple form field; {@code false} if
   * it represents an uploaded file.
   * @return this file item
   */
  @Override
  public InternalFileItem setFormField(boolean formField) {
    return this;
  }

  /**
   * Does nothing.
   *
   * @param path The {@code File} into which the uploaded item should be stored.
   * @return this file item.
   * @throws IOException if an error occurs while writing this file item content into the file
   * specified by the given path.
   */
  @Override
  public InternalFileItem write(Path path) throws IOException {
    return this;
  }

  /**
   * Is this file item a field of a form?
   *
   * @return true.
   */
  @Override
  public boolean isFormField() {
    return true;
  }

  /**
   * Does nothing.
   *
   * @return itself.
   */
  @Override
  public InternalFileItem delete() {
    return this;
  }

  @Override
  public byte[] get() {
    return value.getBytes(Charsets.UTF_8);
  }

  /**
   * The content type of this file item.
   *
   * @return null as no content type is associated to this form field.
   */
  @Override
  public String getContentType() {
    return null;
  }

  /**
   * Gets an input stream to the file item content.
   *
   * @return null as there is no content. This file item is a field form.
   */
  @Override
  public InputStream getInputStream() {
    return null;
  }

  /**
   * Gets the field name
   *
   * @return the name of the form field.
   */
  @Override
  public String getName() {
    return fieldName;
  }

  /**
   * Gets an output stream on the content of this file item.
   *
   * @return null as there is no content. This file item is a field of a form in Silverpeas.
   */
  @Override
  public OutputStream getOutputStream() {
    return null;
  }

  /**
   * Gets the size of the content of this file item.
   *
   * @return 0 as there is no content.
   */
  @Override
  public long getSize() {
    return 0;
  }

  /**
   * Is this file item in memory?
   *
   * @return false.
   */
  @Override
  public boolean isInMemory() {
    return false;
  }

  /**
   * Gets the headers of this file item.
   *
   * @return null.
   */
  @Override
  public FileItemHeaders getHeaders() {
    return null;
  }

  /**
   * Does nothing.
   *
   * @param fileItemHeaders the instance that holds onto the headers for this instance.
   * @return itself.
   */
  @Override
  public InternalFileItem setHeaders(final FileItemHeaders fileItemHeaders) {
    return this;
  }
}