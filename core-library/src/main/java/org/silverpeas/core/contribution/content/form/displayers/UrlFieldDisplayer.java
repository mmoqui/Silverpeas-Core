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
package org.silverpeas.core.contribution.content.form.displayers;

import org.silverpeas.core.contribution.content.form.Field;
import org.silverpeas.core.contribution.content.form.FieldDisplayer;
import org.silverpeas.core.contribution.content.form.FieldTemplate;
import org.silverpeas.core.contribution.content.form.Form;
import org.silverpeas.core.contribution.content.form.FormException;
import org.silverpeas.core.contribution.content.form.PagesContext;
import org.silverpeas.core.contribution.content.form.Util;
import org.silverpeas.core.contribution.content.form.field.TextField;
import org.silverpeas.core.contribution.content.form.field.TextFieldImpl;
import org.apache.ecs.ElementContainer;
import org.apache.ecs.xhtml.img;
import org.apache.ecs.xhtml.input;
import org.silverpeas.core.util.WebEncodeHelper;
import org.silverpeas.kernel.util.StringUtil;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * A TextFieldDisplayer is an object which can display a TextField in HTML the content of a
 * TextField to a end user and can retrieve via HTTP any updated value.
 *
 * @see Field
 * @see FieldTemplate
 * @see Form
 * @see FieldDisplayer
 */
public class UrlFieldDisplayer extends AbstractTextFieldDisplayer {

  /**
   * Prints the HTML value of the field. The displayed value must be updatable by the end user. The
   * value format may be adapted to a local language. The fieldName must be used to name the html
   * form input. Never throws an Exception but log a silvertrace and writes an empty string when :
   * <ul>
   * <li>the field type is not a managed type.</li>
   * </ul>
   */
  @Override
  public void display(PrintWriter out, TextField field, FieldTemplate template,
      PagesContext pageContext) throws FormException {
    String html = "";
    String fieldName = Util.getFieldOccurrenceName(template.getFieldName(), field.getOccurrence());
    Map<String, String> parameters = template.getParameters(pageContext.getLanguage());

    String defaultValue = getDefaultValue(template, pageContext);
    String value = (!field.isNull() ? field.getValue(pageContext.getLanguage()) : defaultValue);
    if (pageContext.isBlankFieldsUse()) {
      value = "";
    }

    if (template.isReadOnly() && !template.isHidden()) {
      if (StringUtil.isDefined(value)) {
        if (!value.startsWith("http") && !value.startsWith("ftp:") && !value.startsWith("/")) {
          value = "http://" + value;
        }
        html =
            "<a target=\"_blank\" href=\"" + value + "\">"
            + WebEncodeHelper.javaStringToHtmlString(value) + "</a>";
      }
    } else {
      // Suggestions used ?
      String paramSuggestions =
          parameters.getOrDefault("suggestions", "false");
      boolean useSuggestions = Boolean.parseBoolean(paramSuggestions);
      List<String> suggestions =
          getSuggestions((TextFieldImpl) field, template, pageContext, fieldName, useSuggestions);

      input inputField = getInputField(template, fieldName, parameters, value);
      img image = getImage(template, pageContext);

      if (suggestions != null && !suggestions.isEmpty()) {
        printSuggestions(out, fieldName, suggestions, inputField, image);
      } else {
        // print fields
        printFields(out, inputField, image);
      }
    }
    out.println(html);
  }

  private static void printFields(final PrintWriter out, final input inputField, final img image) {
    if (image != null) {
      ElementContainer container = new ElementContainer();
      container.addElement(inputField);
      container.addElement("&nbsp;");
      container.addElement(image);
      out.println(container);
    } else {
      out.println(inputField);
    }
  }

  private static void printSuggestions(final PrintWriter out, final String fieldName,
      final List<String> suggestions, final input inputField, final img image) {
    TextFieldImpl.printSuggestionsIncludes(fieldName, out);
    out.println("<div id=\"listAutocomplete" + fieldName + "\">\n");

    out.println(inputField);

    out.println("<div id=\"container" + fieldName + "\"/>\n");
    out.println("</div>\n");

    if (image != null) {
      image.setStyle("position:absolute;left:16em;top:5px");
      out.println(image);
    }

    TextFieldImpl.printSuggestionsScripts(fieldName, suggestions, out);
  }

  private static img getImage(final FieldTemplate template, final PagesContext pageContext) {
    img image = null;
    if (template.isMandatory() && !template.isDisabled() && !template.isReadOnly()
        && !template.isHidden() && pageContext.useMandatory()) {
      image = new img();
      image.setSrc(Util.getIcon("mandatoryField"));
      image.setWidth(5);
      image.setHeight(5);
      image.setBorder(0);
    }
    return image;
  }

  private static input getInputField(final FieldTemplate template, final String fieldName,
      final Map<String, String> parameters, final String value) {
    input inputField = new input();
    inputField.setName(fieldName);
    inputField.setID(fieldName);
    inputField.setValue(WebEncodeHelper.javaStringToHtmlString(value));
    inputField.setType(template.isHidden() ? input.hidden : input.text);
    inputField.setMaxlength(parameters.getOrDefault("maxLength", "1000"));
    inputField.setSize(parameters.getOrDefault("size", "50"));
    if (parameters.containsKey("border")) {
      inputField.setBorder(Integer.parseInt(parameters.get("border")));
    }
    if (template.isDisabled()) {
      inputField.setDisabled(true);
    } else if (template.isReadOnly()) {
      inputField.setReadOnly(true);
    }
    return inputField;
  }

  private static List<String> getSuggestions(final TextFieldImpl field, final FieldTemplate template,
      final PagesContext pageContext, final String fieldName, final boolean useSuggestions) {
    List<String> suggestions = null;
    if (useSuggestions) {
      suggestions =
          field.getSuggestions(fieldName, template.getTemplateName(), pageContext.
          getComponentId());
    }
    return suggestions;
  }
}