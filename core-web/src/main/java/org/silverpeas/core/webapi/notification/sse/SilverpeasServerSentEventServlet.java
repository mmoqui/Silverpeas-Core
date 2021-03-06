/*
 * Copyright (C) 2000 - 2021 Silverpeas
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.core.webapi.notification.sse;

import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.notification.sse.ServerEvent;
import org.silverpeas.core.notification.sse.SilverpeasAsyncContext;
import org.silverpeas.core.notification.sse.SilverpeasAsyncContextManager;
import org.silverpeas.core.notification.sse.SseLogger;
import org.silverpeas.core.security.session.SessionInfo;
import org.silverpeas.core.util.Pair;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.util.logging.SilverLogger;
import org.silverpeas.core.web.http.HttpRequest;
import org.silverpeas.core.web.mvc.controller.MainSessionController;
import org.silverpeas.core.web.mvc.webcomponent.SilverpeasAuthenticatedHttpServlet;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.Math.min;
import static java.text.MessageFormat.format;
import static org.silverpeas.core.notification.sse.ServerEventDispatcherTask.getLastServerEventsFromId;
import static org.silverpeas.core.notification.sse.ServerEventDispatcherTask.registerAsyncContext;
import static org.silverpeas.core.notification.sse.SilverpeasAsyncContext.wrap;
import static org.silverpeas.core.notification.user.client.NotificationManagerSettings.getSseAsyncTimeout;
import static org.silverpeas.core.notification.user.client.NotificationManagerSettings.isCheckPreviousAsyncContextEnabled;
import static org.silverpeas.core.security.session.SessionManagementProvider.getSessionManagement;

/**
 * This abstraction defines the behavior the Servlets in charge of responding to EventSource
 * JavaScript API must have.<br>
 * Finally, adding a new URI to handle EventSource needs to extends this class and to parametrize
 * the mapping! (see {@link CommonServerSentEventServlet} as an example)
 * @author Yohann Chastagnier
 */
public abstract class SilverpeasServerSentEventServlet extends SilverpeasAuthenticatedHttpServlet {
  private static final long serialVersionUID = -4766652077117461779L;

  private static final Set<String> sseUri = Collections.synchronizedSet(new LinkedHashSet<>());
  private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";
  private static final String HEARTBEAT_PARAM = "heartbeat";
  private static final String LAST_EVENT_ID_PARAM = "lastEventId";

  /**
   * Indicates if the given request is an SSE one.
   * <p>If the servlet has not been called at least one time, this method will always return
   * true, even if the request is an SSE one</p>
   * @param request the request to check.
   * @return true if an SSE one, false otherwise.
   */
  public static boolean isSseRequest(HttpServletRequest request) {
    return sseUri.contains(request.getRequestURI());
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse response) {
    HttpRequest request = HttpRequest.decorate(req);
    sseUri.add(request.getRequestURI());
    final SilverLogger silverLogger = SseLogger.get();

    final String requestURI = request.getRequestURI();
    final MainSessionController mainSessionController = getMainSessionController(request);
    final String mainSessionId = mainSessionController.getSessionId();
    final User sessionUser = mainSessionController.getCurrentUserDetail();
    final String userSessionId;
    if (sessionUser.isAnonymous()) {
      userSessionId = mainSessionId;
    } else {
      final SessionInfo sessionInfo = getSessionManagement().getSessionInfo(mainSessionController.getSessionId());
      userSessionId = sessionInfo.getSessionId();
    }

    if (!"text/event-stream".equals(request.getHeader("Accept"))) {
      final String errorMessage =
          "Server Sent Servlet accepts only 'test/event-stream' requests ({0})";
      silverLogger.error(errorMessage, requestURI);
      throwHttpForbiddenError(errorMessage);
    }

    silverLogger.debug("Asking for SSE communication (sessionUser={0}) on URI {1} (SessionId={2})",
        sessionUser.getId(), requestURI, userSessionId);

    // Configuring the response
    response.setContentType("text/event-stream");
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("Connection", "keep-alive");
    response.setHeader("X-Accel-Buffering", "no");
    response.setCharacterEncoding("UTF-8");

    // Is heartbeat requested? (first opening)
    boolean heartbeat = StringUtil.getBooleanValue(request.getParameter(HEARTBEAT_PARAM));

    // An initial response
    Long lastServerEventId;
    try {
      lastServerEventId = Long.valueOf(request.getHeader(LAST_EVENT_ID_HEADER));
    } catch (NumberFormatException ignore) {
      lastServerEventId = request.getParameterAsLong(LAST_EVENT_ID_PARAM);
      if (lastServerEventId != null) {
        // When the last server event identifier is retrieved from parameters, that is the
        // polyfill event source implementation behind.
        // Heartbeat is required in a such case.
        heartbeat = true;
      }
    }
    final ServerEvent serverEvent;
    final List<ServerEvent> notConsumedServerEvent = new ArrayList<>();
    if (lastServerEventId != null) {
      silverLogger.debug(
          () -> format("Sending emitted events since disconnection for sessionId {0} on URI {1}",
              request.getSession(false).getId(), requestURI));
      serverEvent = RetryServerEvent.createFor(userSessionId, lastServerEventId);
      final Pair<Long, List<ServerEvent>> result = getLastServerEventsFromId(lastServerEventId);
      lastServerEventId = result.getFirst();
      notConsumedServerEvent.addAll(result.getSecond());
    } else {
      serverEvent = InitializationServerEvent.createFor(userSessionId);
    }

    try {
      if (!request.isAsyncStarted()) {
        // Start Async processing
        final AsyncContext startedAsyncContext = request.startAsync();
        final SilverpeasAsyncContext asyncContext = wrap(silverLogger, startedAsyncContext, userSessionId, sessionUser);
        final int userSessionTimeout = request.getSession(false).getMaxInactiveInterval() * 1000;
        final int asyncTimeout = min(getSseAsyncTimeout(), userSessionTimeout);
        asyncContext.setTimeout(asyncTimeout);
        asyncContext.setLastServerEventId(lastServerEventId);
        asyncContext.setHeartbeat(heartbeat);
        send(asyncContext, serverEvent, notConsumedServerEvent);
      } else {
        silverLogger.warn("Strange that the asynchronous context is already started {0}", request.getAsyncContext());
        response.setStatus(HttpServletResponse.SC_CONFLICT);
      }
    } catch (IOException | ServletException | IllegalStateException e) {
      SilverLogger.getLogger(this).error(e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void send(final SilverpeasAsyncContext asyncContext,
      final ServerEvent serverEvent, final List<ServerEvent> notConsumedServerEvent)
      throws IOException {
    final HttpServletRequest httpRequest = asyncContext.getRequest();
    final HttpServletResponse httpResponse = asyncContext.getResponse();
    final String sessionId = asyncContext.getSessionId();
    final User user = asyncContext.getUser();
    serverEvent.send(httpRequest, httpResponse, sessionId, user);
    for (ServerEvent toSendAgain : notConsumedServerEvent) {
      boolean sent = toSendAgain.send(httpRequest, httpResponse, sessionId, user);
      if (sent) {
        SseLogger.get().debug(() -> format("Send of not consumed {0}", toSendAgain));
      }
    }
    if (isCheckPreviousAsyncContextEnabled()) {
      // trying to close previous opened SSE connexion
      Optional.of(serverEvent)
          .filter(InitializationServerEvent.class::isInstance)
          .stream()
          .flatMap(s -> SilverpeasAsyncContextManager.get().getAsyncContextSnapshot().stream())
          .filter(c -> sessionId.equals(c.getSessionId()))
          .filter(SilverpeasAsyncContext::isSendPossible)
          .forEach(c -> c.safeWrite(() -> {
            try {
              SseLogger.get().debug("send check to {0}", c);
              SessionPreviousCheckServerEvent.createFor(c.getSessionId())
                  .send(c.getRequest(), c.getResponse(), c.getSessionId(), c.getUser());
            } catch (Exception e) {
              c.complete();
            }
          }));
    }
    registerAsyncContext(asyncContext);
  }
}
