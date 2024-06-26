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
package org.silverpeas.core.web.mvc;

import org.silverpeas.core.cache.service.CacheAccessorProvider;
import org.silverpeas.core.cache.service.SessionCacheAccessor;
import org.silverpeas.core.security.session.SessionInfo;
import org.silverpeas.core.security.session.SessionManagement;
import org.silverpeas.core.util.URLUtil;
import org.silverpeas.core.web.mvc.controller.MainSessionController;
import org.silverpeas.core.web.session.HTTPSessionInfo;
import org.silverpeas.core.webapi.notification.sse.SilverpeasServerSentEventServlet;
import org.silverpeas.kernel.logging.SilverLogger;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * This class contains all Silverpeas's needs around WEB application lifecycles.
 */
public class SilverListener
    implements HttpSessionListener, ServletContextListener, ServletRequestListener {

  @Inject
  private SessionManagement sessionManager;

  // HttpSessionListener methods
  @Override
  public void sessionCreated(HttpSessionEvent event) {
    // Nothing to do
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent event) {
    remove(event);
  }

  // ServletContextListener methods
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    // Nothing to do
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    // Nothing to do
  }

  @Override
  public void requestDestroyed(final ServletRequestEvent sre) {
    // Clearing cache at this level avoids memory leaks
    clearThreadCache();
  }

  @Override
  public void requestInitialized(final ServletRequestEvent sre) {
    // Clearing cache at this level ensures that it is cleared before that all treatments behind the
    // request are performed.
    clearThreadCache();
    // Managing the session cache.
    ServletRequest request = sre.getServletRequest();
    // Check an http servlet request
    if (!(request instanceof HttpServletRequest)) {
      return;
    }
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    URLUtil.setCurrentServerUrl(httpRequest);
    if (SilverpeasServerSentEventServlet.isSseRequest(httpRequest)) {
      // Server Sent Event request are detached from Silverpeas HTTP tools
      return;
    }
    // Check HTTP session available
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession == null) {
      return;
    }

    // Setting the context according to the Silverpeas session state
    final SessionCacheAccessor sessionCacheAccessor =
        CacheAccessorProvider.getSessionCacheAccessor();
    final SessionInfo sessionInfo = sessionManager.getSessionInfo(httpSession.getId());
    final Runnable setupSessionCache;
    if (sessionInfo.isDefined()) {
      if (sessionInfo instanceof HTTPSessionInfo && !sessionInfo.isAnonymous()) {
        // a non-anonymous HTTP session
        ((HTTPSessionInfo) sessionInfo).setHttpSession(httpSession);
      }
      setupSessionCache = () -> sessionCacheAccessor.setCurrentSessionCache(sessionInfo.getCache());
    } else {
      // Anonymous management
      MainSessionController mainSessionController = MainSessionController.getInstance(httpSession);
      if (mainSessionController != null && mainSessionController.getCurrentUserDetail() != null &&
          mainSessionController.getCurrentUserDetail().isAnonymous()) {
        // In that case of anonymous access, session cache is handled as a request one in order
        // to avoid concurrency access.
        setupSessionCache = () -> sessionCacheAccessor.newSessionCache(mainSessionController.getCurrentUserDetail());
      } else {
        // shouldn't be executed
        SilverLogger.getLogger(this)
            .debug("No identified user session attached to request ''{0}'' ",
                httpRequest.getRequestURI());
        return;
      }
    }
    try {
      setupSessionCache.run();
    } catch (RuntimeException e) {
      SilverLogger.getLogger(this)
          .warn("request ''{0}'' accessing attributes on closed session ({1})",
              httpRequest.getRequestURI(), e.getMessage(), e);
    }
  }

  // Clear session information
  private void remove(HttpSessionEvent event) {
    final String sessionId = event.getSession().getId();
    sessionManager.closeSession(sessionId);
    SilverLogger.getLogger(this).debug("Session with id {0} has just been closed", sessionId);
  }

  /**
   * Clears the cache associated to the request.
   */
  private void clearThreadCache() {
    CacheAccessorProvider.getThreadCacheAccessor().getCache().clear();
  }
}
