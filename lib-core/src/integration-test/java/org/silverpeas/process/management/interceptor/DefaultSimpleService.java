/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.process.management.interceptor;

import com.stratelia.silverpeas.silvertrace.SilverTrace;
import org.silverpeas.process.annotation.SimulationActionProcess;
import org.silverpeas.util.ActionType;
import org.silverpeas.util.ForeignPK;
import org.silverpeas.util.annotation.Action;
import org.silverpeas.util.annotation.SourceObject;
import org.silverpeas.util.annotation.TargetPK;

import javax.inject.Singleton;

/**
 * @author Yohann Chastagnier
 */
@Singleton
public class DefaultSimpleService implements SimpleService {

  @SimulationActionProcess(elementLister = InterceptorTestFileElementLister.class)
  @Action(ActionType.CREATE)
  @Override
  public InterceptorTestFile create(@SourceObject final InterceptorTestFile file,
      @TargetPK final ForeignPK destination) {
    SilverTrace.info("InterceptorTest", "DefaultSimpleService", "create called");
    return null;
  }

  @SimulationActionProcess(elementLister = InterceptorTestFileElementLister.class)
  @Action(ActionType.DELETE)
  @Override
  public void delete(@SourceObject final InterceptorTestFile file, final ForeignPK destination) {
    SilverTrace.info("InterceptorTest", "DefaultSimpleService", "delete called");
  }

  @SimulationActionProcess(elementLister = InterceptorTestFileElementLister.class)
  @Action(ActionType.MOVE)
  @Override
  public void move(final ForeignPK from, final ForeignPK destination) {
    SilverTrace.info("InterceptorTest", "DefaultSimpleService", "move called");
  }
}