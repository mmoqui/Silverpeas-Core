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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.silverpeas.core.web.mvc.controller;

import org.silverpeas.core.admin.component.model.ComponentInstLight;
import org.silverpeas.core.admin.component.model.PersonalComponentInstance;
import org.silverpeas.core.admin.service.OrganizationController;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.i18n.I18NHelper;
import org.silverpeas.core.ui.DisplayI18NHelper;
import org.silverpeas.core.util.ArrayUtil;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.kernel.util.StringUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static org.silverpeas.kernel.util.StringUtil.isNotDefined;

/**
 * @author ehugonnet
 */
@Singleton
public class SilverpeasWebUtil {

  @Inject
  private OrganizationController organizationController;

  public static SilverpeasWebUtil get() {
    return ServiceProvider.getService(SilverpeasWebUtil.class);
  }

  protected SilverpeasWebUtil() {
  }

  public OrganizationController getOrganisationController() {
    return organizationController;
  }

  /**
   * Accessing the MainSessionController
   *
   * @param request the HttpServletRequest
   * @return the current MainSessionController.
   */
  public MainSessionController getMainSessionController(HttpServletRequest request) {
    return (MainSessionController) request.getSession().getAttribute(
        MainSessionController.MAIN_SESSION_CONTROLLER_ATT);
  }

  /**
   * Extract the space id and the component id.
   *
   * @param request
   * @return
   */
  public String[] getComponentId(HttpServletRequest request) {
    String spaceId;
    String componentId;
    String function;
    String pathInfo = request.getPathInfo();

    if (pathInfo != null) {
      spaceId = null;
      pathInfo = pathInfo.substring(1); // remove first '/'
      function = pathInfo.substring(pathInfo.indexOf('/') + 1, pathInfo.length());
      if (pathInfo.startsWith("jsp")) {
        // Pour les feuilles de styles, icones, ... + Pour les composants de
        // l'espace personnel (non instanciables)
        componentId = null;
      } else {
        // Get the space and component Ids
        // componentId extracted from the URL
        // Old url (with WA..)
        if (pathInfo.contains("WA")) {
          String sAndCId = pathInfo.substring(0, pathInfo.indexOf('/'));
          // spaceId looks like WA17
          spaceId = sAndCId.substring(0, sAndCId.indexOf('_'));
          // componentId looks like kmelia123
          componentId = sAndCId.substring(spaceId.length() + 1, sAndCId.length());
        } else {
          componentId = pathInfo.substring(0, pathInfo.indexOf('/'));
        }

        if ((function.startsWith("Main") || function.startsWith("searchResult") ||
            function.equalsIgnoreCase("searchresult") || function.startsWith("portlet") ||
            function.equals("GoToFilesTab")) &&
            !PersonalComponentInstance.from(componentId).isPresent()) {
          ComponentInstLight component =
              getOrganisationController().getComponentInstLight(componentId);
          spaceId = component.getDomainFatherId();
        }
      }
    } else {
      spaceId = "-1";
      componentId = "-1";
      function = "Error";
    }
    return new String[]{spaceId, componentId, function};
  }

  public String[] getRoles(HttpServletRequest request) {
    MainSessionController controller = getMainSessionController(request);
    if (controller != null) {
      return getOrganisationController()
          .getUserProfiles(controller.getUserId(), getComponentId(request)[1]);
    }
    return ArrayUtil.emptyStringArray();
  }

  /**
   * Gets the content language specified into the request.
   * @param request
   * @return
   */
  public String getContentLanguage(HttpServletRequest request) {
    String contentLanguage = (String) request.getAttribute("ContentLanguage");
    if (StringUtil.isNotDefined(contentLanguage)) {
      contentLanguage = request.getParameter("ContentLanguage");
    }
    if (StringUtil.isNotDefined(contentLanguage)) {
      contentLanguage = I18NHelper.DEFAULT_LANGUAGE;
    }
    if (StringUtil.isNotDefined(contentLanguage)) {
      MainSessionController mainSessionCtrl = getMainSessionController(request);
      if (mainSessionCtrl != null) {
        contentLanguage = mainSessionCtrl.getFavoriteLanguage();
      }
    }
    return contentLanguage;
  }

  /**
   * Gets the user favorite language from elements of the given request.
   * @param request an instance of {@link HttpServletRequest}.
   * @return a string representing the favorite language of the user behind the session or the
   * language of WEB browser (or default language).
   */
  public String getUserLanguage(HttpServletRequest request) {
    return ofNullable(User.getCurrentRequester())
        .filter(not(User::isAnonymous))
        .map(r -> r.getUserPreferences().getLanguage())
        .orElseGet(() -> {
          String userLanguage = (String) request.getAttribute("language");
          if (isNotDefined(userLanguage)) {
            userLanguage = (String) request.getAttribute("userLanguage");
          }
          if (isNotDefined(userLanguage) && request.getLocale() != null) {
            userLanguage = request.getLocale().getLanguage();
          }
          return DisplayI18NHelper.verifyLanguage(userLanguage);
        });
  }
}
