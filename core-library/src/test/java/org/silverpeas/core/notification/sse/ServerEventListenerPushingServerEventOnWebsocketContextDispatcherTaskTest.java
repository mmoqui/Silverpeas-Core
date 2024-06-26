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
package org.silverpeas.core.notification.sse;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.silverpeas.core.thread.task.RequestTaskManager;
import org.silverpeas.kernel.logging.SilverLogger;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * PLEASE NOTICE THAT IT EXISTS TWO LISTENERS...
 * @author Yohann Chastagnier
 */
@EnableAutoWeld
@AddBeanClasses(DefaultServerEventNotifier.class)
@AddPackages({AbstractServerEventDispatcherTaskTest.class, RequestTaskManager.class})
class ServerEventListenerPushingServerEventOnWebSocketContextDispatcherTaskTest
    extends AbstractServerEventDispatcherTaskTest {

  TestServerEventBucket bucket;

  TestServerEventNotifier testServerEventNotifier;

  DefaultServerEventNotifier defaultServerEventNotifier;

  @BeforeEach
  @AfterEach
  // parameters injected by weld
  public void bucketSetup(TestServerEventBucket bucket,
      TestServerEventNotifier testSseNotifier,
      DefaultServerEventNotifier sseNotifier) {
    this.bucket = bucket;
    this.testServerEventNotifier = testSseNotifier;
    this.defaultServerEventNotifier = sseNotifier;
    this.bucket.empty();
  }

  @Test
  void notifyOneServerEventAFromAWhenOneWebSocketContext() {
    final SilverpeasWebSocketContext mockedWebSocketContext = newMockedWebSocketContext("SESSION_ID");
    ServerEventDispatcherTask.registerContext(mockedWebSocketContext);
    TestServerEventA serverEventA = new TestServerEventA();
    SilverLogger.getLogger(this).info("'A' NOTIFY - EVENT 'A'");
    testServerEventNotifier.notify(serverEventA);
    afterSomeTimesCheck(() -> {
      assertThat(getStoredServerEvents(), contains(serverEventA));
      String eventStream = getSentServerMessage(mockedWebSocketContext);
      assertThat(eventStream, is("{\"name\":\"EVENT_A\",\"id\":0,\"data\":\"\"}"));
      assertThat(bucket.getServerEvents(), contains(serverEventA, serverEventA));
    });
  }

  @Test
  void notifyTwoATwoBTwoCTwoDFromAWhenOneWebSocketContext() {
    final SilverpeasWebSocketContext mockedWebSocketContext = newMockedWebSocketContext("SESSION_ID");
    ServerEventDispatcherTask.registerContext(mockedWebSocketContext);
    TestServerEventA serverEventA = new TestServerEventA();
    TestServerEventB serverEventB = new TestServerEventB();
    TestServerEventCNotHandled serverEventC = new TestServerEventCNotHandled();
    TestServerEventDEventSourceURI serverEventD = new TestServerEventDEventSourceURI();
    SilverLogger.getLogger(this).info("'A' NOTIFY - EVENT 'A'");
    testServerEventNotifier.notify(serverEventA);
    SilverLogger.getLogger(this).info("'A' NOTIFY - EVENT 'A'");
    testServerEventNotifier.notify(serverEventA);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'B'");
    defaultServerEventNotifier.notify(serverEventB);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'B'");
    defaultServerEventNotifier.notify(serverEventB);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'C' (not handled)");
    defaultServerEventNotifier.notify(serverEventC);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'C' (not handled)");
    defaultServerEventNotifier.notify(serverEventC);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'D' (event source URI)");
    defaultServerEventNotifier.notify(serverEventD);
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'D' (event source URI)");
    defaultServerEventNotifier.notify(serverEventD);
    afterSomeTimesCheck(() -> {
      assertThat(getStoredServerEvents(), contains(serverEventA, serverEventA, serverEventC));
      String eventStream = getSentServerMessage(mockedWebSocketContext, 4);
      assertThat(eventStream,
          is("{\"name\":\"EVENT_A\",\"id\":0,\"data\":\"\"}" +
              "{\"name\":\"EVENT_A\",\"id\":0,\"data\":\"\"}" +
              "{\"name\":\"EVENT_D\",\"id\":2,\"data\":\"\"}" +
              "{\"name\":\"EVENT_D\",\"id\":2,\"data\":\"\"}"));
      assertThat(bucket.getServerEvents(),
          contains(serverEventA, serverEventA, serverEventA, serverEventA, serverEventB, serverEventB,
              serverEventC, serverEventC, serverEventC, serverEventC, serverEventD, serverEventD,
              serverEventD, serverEventD));
    });
  }

  @Test
  void notifyOneServerEventBAndOneAFromAWhenSeveralWebSocketContext() {
    SilverpeasWebSocketContext[] mockedWebSocketContexts =
        {newMockedWebSocketContext("SESSION_ID"), newMockedWebSocketContext("SESSION_ID"),
            newMockedWebSocketContext("SESSION_ID")};
    for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
      ServerEventDispatcherTask.registerContext(websocketContext);
    }
    TestServerEventA serverEventA = new TestServerEventA().withData("Some data!");
    TestServerEventB serverEventB = new TestServerEventB();
    SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'B'");
    defaultServerEventNotifier.notify(serverEventB);
    SilverLogger.getLogger(this).info("'A' NOTIFY - EVENT 'A'");
    testServerEventNotifier.notify(serverEventA);
    afterSomeTimesCheck(() -> {
      assertThat(getStoredServerEvents(), contains(serverEventA));
      for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
        String eventStream = getSentServerMessage(websocketContext);
        assertThat(eventStream, is("{\"name\":\"EVENT_A\",\"id\":0,\"data\":\"Some data!\"}"));
      }
      assertThat(bucket.getServerEvents(), contains(serverEventB, serverEventA, serverEventA));
    });
  }

  @Test
  void notifySeveralServerEventsBAndSeveralAFromAWhenSeveralWebSocketContext() {
    SilverpeasWebSocketContext[] mockedWebSocketContexts =
        {newMockedWebSocketContext("SESSION_ID"), newMockedWebSocketContext("SESSION_ID"),
            newMockedWebSocketContext("SESSION_ID")};
    for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
      ServerEventDispatcherTask.registerContext(websocketContext);
    }
    TestServerEventA serverEventA = new TestServerEventA().withData("Some data!");
    TestServerEventB serverEventB = new TestServerEventB();

    List<ServerEvent> expectedBucketContent = new ArrayList<>();
    List<ServerEvent> expectedLastServerEvents = new ArrayList<>();
    StringBuilder expectedEventStreamForOneWebSocketContext = new StringBuilder();
    final String HTTP_REQUEST_RESPONSE_SEND_TEMPLATE =
        "{\"name\":\"EVENT_A\",\"id\":0,\"data\":\"Some data!\"}";
    final int nbSend = 10;
    for (int i = 0; i < nbSend; i++) {
      SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'B'");
      defaultServerEventNotifier.notify(serverEventB);
      SilverLogger.getLogger(this).info("'A' NOTIFY - EVENT 'A'");
      testServerEventNotifier.notify(serverEventA);
      expectedBucketContent.add(serverEventB);
      expectedBucketContent.add(serverEventA);
      expectedBucketContent.add(serverEventA);
      expectedLastServerEvents.add(serverEventA);
      expectedEventStreamForOneWebSocketContext.append(HTTP_REQUEST_RESPONSE_SEND_TEMPLATE);
    }
    assertThat(expectedBucketContent, hasSize(nbSend * 3));
    afterSomeTimesCheck(() -> {
      assertThat(getStoredServerEvents(), contains(
          expectedLastServerEvents.toArray(new ServerEvent[0])));
      for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
        String eventStream = getSentServerMessage(websocketContext, nbSend);
        assertThat(eventStream, is(expectedEventStreamForOneWebSocketContext.toString()));
      }
      assertThat(bucket.getServerEvents(),
          contains(expectedBucketContent.toArray(new ServerEvent[0])));
    });
  }

  @Test
  void notifySeveralServerEventsOnCommonNotifierWhenSeveralWebSocketContext() {
    SilverpeasWebSocketContext[] mockedWebSocketContexts =
        {newMockedWebSocketContext("SESSION_ID"), newMockedWebSocketContext("SESSION_ID"),
            newMockedWebSocketContext("SESSION_ID")};
    for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
      ServerEventDispatcherTask.registerContext(websocketContext);
    }

    List<ServerEvent> expectedBucketContent = new ArrayList<>();
    List<ServerEvent> expectedLastServerEvents = new ArrayList<>();
    StringBuilder expectedEventStreamForOneWebSocketContext = new StringBuilder();
    final String HTTP_REQUEST_RESPONSE_SEND_TEMPLATE =
        "{\"name\":\"EVENT_A\",\"id\":%d,\"data\":\"Data \\n A %d\"}";
    final int nbSend = 10;
    for (int i = 0; i < nbSend; i++) {
      TestServerEventA serverEventA = new TestServerEventA().withData("Data \n A " + i);
      TestServerEventB serverEventB = new TestServerEventB().withData("Data B " + i);
      SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'B'");
      defaultServerEventNotifier.notify(serverEventB);
      SilverLogger.getLogger(this).info("'COMMON' NOTIFY - EVENT 'A'");
      defaultServerEventNotifier.notify(serverEventA);
      expectedBucketContent.add(serverEventB);
      expectedBucketContent.add(serverEventA);
      expectedBucketContent.add(serverEventA);
      expectedLastServerEvents.add(serverEventA);
      expectedEventStreamForOneWebSocketContext.append(HTTP_REQUEST_RESPONSE_SEND_TEMPLATE.replace("%d", String.valueOf(i)));
    }
    assertThat(expectedBucketContent, hasSize(nbSend * 3));
    afterSomeTimesCheck(() -> {
      assertThat(getStoredServerEvents(), contains(
          expectedLastServerEvents.toArray(new ServerEvent[0])));
      for (SilverpeasWebSocketContext websocketContext : mockedWebSocketContexts) {
        String eventStream = getSentServerMessage(websocketContext, nbSend);
        assertThat(eventStream, is(expectedEventStreamForOneWebSocketContext.toString()));
      }
      assertThat(bucket.getServerEvents(),
          contains(expectedBucketContent.toArray(new ServerEvent[0])));
    });
  }
}