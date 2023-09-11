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
package org.silverpeas.core.contribution.content.form.displayers;

import org.silverpeas.core.contribution.content.form.Field;
import org.silverpeas.core.contribution.content.form.FieldDisplayer;
import org.silverpeas.core.contribution.content.form.FieldTemplate;
import org.silverpeas.core.contribution.content.form.Form;
import org.silverpeas.core.contribution.content.form.FormException;
import org.silverpeas.core.contribution.content.form.PagesContext;
import org.silverpeas.core.contribution.content.form.Util;
import org.silverpeas.core.contribution.content.form.field.TextField;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.util.logging.SilverLogger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A RadioButtonDisplayer is an object which can display a radio button in HTML the content of a
 * radio button to a end user and can retrieve via HTTP any updated value.
 *
 * @see Field
 * @see FieldTemplate
 * @see Form
 * @see FieldDisplayer
 */
public class RadioButtonDisplayer extends AbstractFieldDisplayer<TextField> {

  private static final String VALUES = "values";
  private static final String DELIM = "##";

  /**
   * Returns the name of the managed types.
   */
  public String[] getManagedTypes() {
    return new String[] { TextField.TYPE };
  }

  /**
   * Prints the javascript which will be used to control the new value given to the named field.
   * The error messages may be adapted to a local language. The FieldTemplate gives the field type
   * and constraints. The FieldTemplate gives the local labeled too. Never throws an Exception but
   * log a message and writes an empty string when:
   * <UL>
   * <LI>the fieldName is unknown by the template.
   * <LI>the field type is not a managed type.
   * </UL>
   */
  @Override
  public void displayScripts(PrintWriter out, FieldTemplate template, PagesContext pagesContext) {

    String language = pagesContext.getLanguage();
    String fieldName = template.getFieldName();

    if (template.isMandatory() && pagesContext.useMandatory()) {
      out.println(" var checked = $('input[type=radio][name=" + fieldName
          + "]:checked').length == 1;\n");
      out.println(" if(!ignoreMandatory && checked == false) {");
      out.println("   errorMsg+=\"  - '" + template.getLabel(language) + "' " + Util.getString(
          "GML.MustBeFilled", language) + "\\n\";\n");
      out.println("   errorNb++;");
      out.println(" }");
    }

    Util.getJavascriptChecker(template.getFieldName(), pagesContext, out);
  }

  /**
   * Prints the HTML value of the field. The displayed value must be updatable by the end user. The
   * value format may be adapted to a local language. The fieldName must be used to name the html
   * form input. Never throws an Exception but log a silvertrace and writes an empty string when :
   * <UL>
   * <LI>the field type is not a managed type.
   * </UL>
   */
  @Override
  public void display(PrintWriter out, TextField field, FieldTemplate template,
      PagesContext pageContext)
      throws FormException {
    String keys = "";
    String values = "";
    StringBuilder html = new StringBuilder();
    int cols = 1;
    String language = pageContext.getLanguage();
    String cssClass = null;

    String fieldName = template.getFieldName();
    Map<String, String> parameters = template.getParameters(language);

    if (parameters.containsKey("keys")) {
      keys = parameters.get("keys");
    }

    if (parameters.containsKey(VALUES)) {
      values = parameters.get(VALUES);
    }

    if (parameters.containsKey("class")) {
      cssClass = parameters.get("class");
      if (StringUtil.isDefined(cssClass)) {
        cssClass = "class=\"" + cssClass + "\"";
      }
    }

    try {
      if (parameters.containsKey("cols")) {
        cols = Integer.parseInt(parameters.get("cols"));
      }
    } catch (NumberFormatException nfe) {
      SilverLogger.getLogger(this)
          .error("Illegal parameter column: " + parameters.get("cols"), nfe);
    }

    String defaultValue = getDefaultValue(template, pageContext);
    String value = (!field.isNull() ? field.getValue(pageContext.getLanguage()) : defaultValue);

    if (pageContext.isBlankFieldsUse()) {
      value = "";
    }

    // if either keys or values is not filled
    // take the same for keys and values
    if (keys.isEmpty() && !values.isEmpty()) {
      keys = values;
    }
    if (values.isEmpty() && !keys.isEmpty()) {
      values = keys;
    }

    StringTokenizer stKeys = new StringTokenizer(keys, DELIM);
    StringTokenizer stValues = new StringTokenizer(values, DELIM);
    String optKey;
    String optValue;
    int nbTokens = getNbHtmlObjectsDisplayed(template, pageContext);

    if (stKeys.countTokens() == stValues.countTokens()) {
      html.append("<table border=\"0\">");
      int col = 0;
      for (int i = 0; i < nbTokens; i++) {
        if (col == 0) {
          html.append("<tr>");
        }

        col++;
        html.append("<td>");
        optKey = stKeys.nextToken();
        optValue = stValues.nextToken();

        if (StringUtil.isDefined(cssClass)) {
          html.append("<span ").append(cssClass).append(">");
        }
        html.append("<input type=\"radio\" id=\"")
            .append(fieldName).append("_").append(i).append("\" ")
            .append("name=\"").append(fieldName).append("\" value=\"").append(optKey).append("\"")
            .append(" ");

        if (template.isDisabled() || template.isReadOnly()) {
          html.append(" disabled=\"disabled\" ");
        }

        if (optKey.equals(value)) {
          html.append(" checked=\"checked\" ");
        }

        html.append("/>&nbsp;").append(optValue);

        if (StringUtil.isDefined(cssClass)) {
          html.append("</span>");
        }

        if (i == nbTokens - 1 &&
            (template.isMandatory() && !template.isDisabled() && !template.isReadOnly()
              && !template.isHidden() && pageContext.useMandatory())) {
            html.append(Util.getMandatorySnippet());

        }

        html.append("</td>");

        if (col == cols) {
          html.append("</tr>");
          col = 0;
        }
      }

      if (col != 0) {
        html.append("</tr>");
      }

      html.append("</table>");
    }
    out.println(html);
  }

  /**
   * Updates the value of the field. The fieldName must be used to retrieve the HTTP parameter from
   * the request.
   *
   * @throws FormException if the field type is not a managed type or if the field doesn't accept
   * the new value.
   */
  @Override
  public List<String> update(String newValue, TextField field, FieldTemplate template,
      PagesContext pagesContext) throws FormException {

    if (!TextField.TYPE.equals(field.getTypeName())) {
      throw new FormException("TextAreaFieldDisplayer.update", "form.EX_NOT_CORRECT_TYPE",
          TextField.TYPE);
    }
    if (field.acceptValue(newValue, pagesContext.getLanguage())) {
      field.setValue(newValue, pagesContext.getLanguage());
    } else {
      throw new FormException("TextAreaFieldDisplayer.update", "form.EX_NOT_CORRECT_VALUE",
          TextField.TYPE);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isDisplayedMandatory() {
    return true;
  }

  @Override
  public int getNbHtmlObjectsDisplayed(FieldTemplate template, PagesContext pagesContext) {
    String keys = "";
    String values = "";
    Map<String, String> parameters = template.getParameters(pagesContext.getLanguage());
    if (parameters.containsKey("keys")) {
      keys = parameters.get("keys");
    }
    if (parameters.containsKey(VALUES)) {
      values = parameters.get(VALUES);
    }

    // if either keys or values is not filled
    // take the same for keys and values
    if (keys.isEmpty() && !values.isEmpty()) {
      keys = values;
    }

    // Calculate numbers of html elements
    StringTokenizer stKeys = new StringTokenizer(keys, DELIM);
    return stKeys.countTokens();
  }
}
