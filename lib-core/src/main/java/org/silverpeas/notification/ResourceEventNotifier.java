/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.notification;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;

/**
 * A notifier of an event about a resource in Silverpeas. A notification can be either synchronous
 * or asynchronous. For each of these two different notification way, an abstract class is defined:
 * <ul>
 *   <li>{@code org.silverpeas.notification.SynchronousResourceEventNotifier} for the synchronous
 *   notification,</li>
 *   <li>{@code org.silverpeas.notification.JMSResourceEventNotifier} for the asynchronous
 *   notification.</li>
 * </ul>
 * @author mmoquillon
 */
public interface ResourceEventNotifier<R extends Serializable, T extends ResourceEvent> {

  /**
   * Notify about the specified event.
   * @param event the event to fire.
   */
  public void notify(T event);

  /**
   * Notify about an event of the specified type and on the specified resource.
   * @param type the type of the event that defines its cause.
   * @param resource the resource related by the event. In the case of an update, two instances of
   * the same resource is expected: the first being the resource before the update, the second
   * being the resource after the update.
   */
  public void notifyEventOn(ResourceEvent.Type type, R... resource);
}