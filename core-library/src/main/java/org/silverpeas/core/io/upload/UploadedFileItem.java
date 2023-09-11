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

package org.silverpeas.core.io.upload;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.ecs.wml.Em;
import org.checkerframework.checker.units.qual.C;
import org.silverpeas.core.util.Charsets;
import org.silverpeas.core.util.file.FileUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.apache.commons.io.Charsets.toCharset;
import static org.silverpeas.core.util.StringUtil.EMPTY;

/**
 * Converts an {@link UploadedFile} into a usable {@link FileItem} instance.
 *
 * @author silveryocha
 */
public class UploadedFileItem implements FileItem<UploadedFileItem> {

  private UploadedFile uploadedFile;
  private FileItemHeaders fileItemHeaders;
  private boolean isFormField;
  private String fieldName;
  private long size;

  UploadedFileItem(final UploadedFile uploadedFile) {
    this.fieldName = "";
    this.isFormField = false;
    this.size = 0;
    this.fileItemHeaders = null;
    this.uploadedFile = uploadedFile;
  }

  @Override
  public String getName() {
    return uploadedFile.getFile().getName();
  }

  @Override
  public long getSize() {
    return uploadedFile.getFile().length();
  }

  /**
   * Gets the contents of the file item as a String, using the default character encoding. This
   * method uses {@link #get()} to retrieve the contents of the item.
   *
   * @return The contents of the item, as a string.
   */
  @Override
  public String getString() {
    return new String(get(), Charsets.UTF_8);
  }

  /**
   * Gets the contents of the file item as a String, using the specified encoding. This method uses
   * {@link #get()} to retrieve the contents of the item.
   *
   * @param toCharset The character encoding to use.
   * @return The contents of the item, as a string.
   * @throws IOException if an I/O error occurs
   */
  @Override
  public String getString(Charset toCharset) throws IOException {
    return new String(get(), toCharset(toCharset, Charsets.UTF_8));
  }

  /**
   * Tests whether or not a {@code FileItem} instance represents a simple form field.
   *
   * @return {@code true} if the instance represents a simple form field; {@code false} if it
   * represents an uploaded file.
   */
  @Override
  public boolean isFormField() {
    return isFormField;
  }

  /**
   * Tests a hint as to whether or not the file contents will be read from memory.
   *
   * @return {@code true} if the file contents will be read from memory; {@code false} otherwise.
   */
  @Override
  public boolean isInMemory() {
    return false;
  }

  /**
   * Sets the field name used to reference this file item.
   *
   * @param name The name of the form field.
   * @return this
   */
  @Override
  public UploadedFileItem setFieldName(String name) {
    this.fieldName = name;
    return this;
  }

  /**
   * Sets whether or not a {@code FileItem} instance represents a simple form field. Does nothing as
   * this file item represents an uploaded file.
   *
   * @param state {@code true} if the instance represents a simple form field; {@code false} if it
   * represents an uploaded file.
   * @return this
   */
  @Override
  public UploadedFileItem setFormField(boolean state) {
    this.isFormField = state;
    return this;
  }

  /**
   * Writes an uploaded item to disk.
   * <p>
   * The client code is not concerned with whether or not the item is stored in memory, or on disk
   * in a temporary location. They just want to write the uploaded item to a file.
   * </p>
   * <p>
   * This method is not guaranteed to succeed if called more than once for the same item. This
   * allows a particular implementation to use, for example, file renaming, where possible, rather
   * than copying all of the underlying data, thus gaining a significant performance benefit.
   * </p>
   *
   * @param file The {@code File} into which the uploaded item should be stored.
   * @return this
   * @throws IOException if an error occurs.
   */
  @Override
  public UploadedFileItem write(Path file) throws IOException {
    final Path outputFile = uploadedFile.getPath();
    if (outputFile == null) {
      /*
       * For whatever reason we cannot write the file to disk.
       */
      throw new FileUploadException("Cannot write uploaded file to disk.");
    }
    // Save the length of the file
    size = Files.size(outputFile);
    //
    // The uploaded file is being stored on disk in a temporary location so move it to the
    // desired file.
    //
    Files.move(outputFile, file, StandardCopyOption.REPLACE_EXISTING);
    return this;
  }

  @Override
  public String getContentType() {
    return FileUtil.getMimeType(getName());
  }

  /**
   * Gets the name of the field in the multipart form corresponding to this file item.
   *
   * @return The name of the form field.
   */
  @Override
  public String getFieldName() {
    return this.fieldName;
  }

  /**
   * Deletes the underlying storage for a file item, including deleting any associated temporary
   * disk file. Use this method to ensure that this is done at an earlier time, to preserve
   * resources.
   *
   * @return this
   * @throws IOException if an error occurs.
   */
  @Override
  public UploadedFileItem delete() throws IOException {
    final Path outputFile = uploadedFile.getPath();
    if (outputFile != null && !isInMemory() && Files.exists(outputFile)) {
      Files.delete(outputFile);
    }
    return this;
  }

  @Override
  public byte[] get() {
    byte[] fileData = new byte[(int) getSize()];
    try (InputStream fis = new FileInputStream(uploadedFile.getFile())) {
      IOUtils.readFully(fis, fileData);
    } catch (IOException e) {
      fileData = null;
    }
    return fileData;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(uploadedFile.getFile());
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new FileOutputStream(uploadedFile.getFile());
  }

  /**
   * Gets the collection of headers defined locally within this item.
   *
   * @return the {@link FileItemHeaders} present for this item.
   */
  @Override
  public FileItemHeaders getHeaders() {
    return this.fileItemHeaders;
  }

  /**
   * Sets the headers read from within an item. Implementations of {@link FileItem} or
   * {@link FileItemInput} should implement this interface to be able to get the raw headers found
   * within the item header block.
   *
   * @param headers the instance that holds onto the headers for this instance.
   * @return this
   */
  @Override
  public UploadedFileItem setHeaders(FileItemHeaders headers) {
    this.fileItemHeaders = headers;
    return null;
  }
}
