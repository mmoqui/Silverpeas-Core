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
package org.silverpeas.core.util.file;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.silverpeas.core.SilverpeasRuntimeException;
import org.silverpeas.core.util.Charsets;
import org.silverpeas.core.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file uploading.
 * @author ehugonnet
 */
public class FileUploadUtil {

  public static final Charset DEFAULT_ENCODING = Charsets.UTF_8;

  private FileUploadUtil() {
  }

  private static final JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload =
      new JakartaServletFileUpload<>(new DiskFileItemFactoryProvider().provide());

  @SuppressWarnings("unused")
  public static boolean isRequestMultipart(HttpServletRequest request) {
    return JakartaServletFileUpload.isMultipartContent(request);
  }

  /**
   * Parses the multipart stream in the specified request to fetch the file items. This method
   * shouldn't be used directly; instead use the HttpRequest instance.
   * @param request the HTTP servlet request.
   * @return a list of file items encoded into the multipart stream of the request.
   * @throws SilverpeasRuntimeException if an error occurs while fetching the file items.
   */
  public static List<DiskFileItem> parseRequest(HttpServletRequest request) {
    try {
      // Parse the request
      return upload.parseRequest(request);
    } catch (FileUploadException e) {
      throw new SilverpeasRuntimeException("Error uploading files", e);
    }
  }

  /**
   * Get the parameter value from the list of FileItems. Returns the defaultValue if the parameter
   * is not found.
   * @param items the items resulting from parsing the request.
   * @param parameterName name of the parameter.
   * @param defaultValue the value to be returned if the parameter is not found.
   * @param charset the request charset.
   * @return the parameter value from the list of FileItems. Returns the defaultValue if the
   * parameter is not found.
   */
  public static <T extends FileItem<T>> String getParameter(List<T> items,
      String parameterName,
      String defaultValue, Charset charset) {
    for (FileItem<?> item : items) {
      if (item.isFormField() && parameterName.equals(item.getFieldName())) {
        try {
          return item.getString(charset);
        } catch (IOException e) {
          return item.getString();
        }
      }
    }
    return defaultValue;
  }

  @SuppressWarnings("unused")
  public static List<String> getParameterValues(List<FileItem<?>> items, String parameterName,
      Charset charset) {
    List<String> values = new ArrayList<>();
    for (FileItem<?> item : items) {
      if (item.isFormField() && item.getFieldName().startsWith(parameterName)) {
        try {
          values.add(item.getString(charset));
        } catch (IOException e) {
          values.add(item.getString());
        }
      }
    }
    return values;
  }

  /**
   * Get the parameter value from the list of FileItems. Returns the defaultValue if the parameter
   * is not found.
   * @param items the items resulting from parsing the request.
   * @param parameterName the name of the parameter.
   * @param defaultValue the value to be returned if the parameter is not found.
   * @return the parameter value from the list of FileItems. Returns the defaultValue if the
   * parameter is not found.
   */
  public static <T extends FileItem<T>> String getParameter(List<T> items,
      String parameterName,
      String defaultValue) {
    return getParameter(items, parameterName, defaultValue, DEFAULT_ENCODING);
  }

  /**
   * Get the parameter value from the list of FileItems. Returns null if the parameter is not found.
   * @param items the items resulting from parsing the request.
   * @param parameterName the name of the parameter.
   * @return the parameter value from the list of FileItems. Returns null if the parameter is not
   * found.
   */
  public static <T extends FileItem<T>> String getParameter(List<T> items,
      String parameterName) {
    return getParameter(items, parameterName, null);
  }

  public static <T extends FileItem<T>> T getFile(List<T> items,
      String parameterName) {
    for (T item : items) {
      if (!item.isFormField() && parameterName.equals(item.getFieldName())) {
        return item;
      }
    }
    return null;
  }

  public static <T extends FileItem<T>> T getFile(List<T> items) {
    for (T item : items) {
      if (!item.isFormField()) {
        return item;
      }
    }
    return null;
  }

  public static DiskFileItem getFile(HttpServletRequest request) {
    List<DiskFileItem> items = FileUploadUtil.parseRequest(request);
    return FileUploadUtil.getFile(items);
  }

  public static String getFileName(FileItem<?> file) {
    if (file == null || !StringUtil.isDefined(file.getName())) {
      return "";
    }
    return FileUtil.getFilename(file.getName());
  }

  @SuppressWarnings("unused")
  public static void saveToFile(File file, FileItem<?> item) throws IOException {
    FileUtils.copyInputStreamToFile(item.getInputStream(), file);
  }
}
