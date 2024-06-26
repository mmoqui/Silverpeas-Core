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
package org.silverpeas.core.scheduler;

import org.silverpeas.kernel.SilverpeasException;

/**
 * A listener of events generating within the scheduling system and about scheduled jobs. All
 * objects that need to be informed about the state of a job has to implement this interface and to
 * be registered in a scheduler with the job to monitor. So, they will receive events for each job
 * triggering and for each job termination (according to the status of this termination: an
 * abnormally termination or successful termination).
 * <p>
 * The {@link SchedulerEventListener} can be taken into account by persistent schedulers as it isn't
 * really persisted. Indeed, only the class name of the listener is serialized so that it can be
 * constructed each time it is being invoked. Therefore, any change in the execution login of the
 * listener will be taken into account. Nevertheless, for doing, the listener has to be stateless
 * and non anonymous and it must define a constructor without parameters. However, the listener
 * can be also managed by the underlying IoC container. Indeed, when fetching from the persistence
 * context, if such a listener is managed by the {@link org.silverpeas.core.util.ServiceProvider},
 * then this is this managed listener that will be used; otherwise it will be constructed.
 * </p>
 * <p>
 * Any listener registered with a job scheduled into a volatile scheduler isn't constrain by the
 * same limitations that a listener registered with a job scheduled in a persistent scheduler: it
 * can be a stateful or an anonymous listener.
 * </p>
 */
public interface SchedulerEventListener {

  /**
   * Invoked when a job trigger fires the execution of a job. The call of this method occurs before
   * the actual job execution. So, whether an error occurs during the processing of this call, it is
   * considered as a job failure and as consequence an event about a job failure will be sent to the
   * listener. The processing of this event can be, for example for preparing the resources before
   * the job execution or performing the execution of the job itself (delegation).
   * @param anEvent the event coming from the trigger firing.
   * @throws SilverpeasException if the process of this event failed.
   */
  void triggerFired(final SchedulerEvent anEvent) throws SilverpeasException;

  /**
   * Invoked when the execution of a job has been completed correctly. The job execution is
   * considered as completed when it ends without raising any exceptions. The processing of this
   * call can be, for example, for freeing the resources after a job completion.
   * @param anEvent the event coming from the job completion.
   */
  void jobSucceeded(final SchedulerEvent anEvent);

  /**
   * Invoked when the normal execution thread of a job is broken by an exception. When an exception
   * is thrown by the job execution, the exception is catched by the scheduler that then considers
   * the job has failed and thus send an event about that failure. The processing of this event can
   * be, for example, for freeing correctly the resources after a job failure or to run a rollback
   * or a retry process.
   * @param anEvent the event coming from the job failure.
   */
  void jobFailed(final SchedulerEvent anEvent);

}
