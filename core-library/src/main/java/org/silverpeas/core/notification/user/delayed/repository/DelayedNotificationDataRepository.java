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
package org.silverpeas.core.notification.user.delayed.repository;

import org.silverpeas.core.notification.user.client.constant.NotifChannel;
import org.silverpeas.core.notification.user.delayed.constant.DelayedNotificationFrequency;
import org.silverpeas.core.notification.user.delayed.model.DelayedNotificationData;
import org.silverpeas.core.persistence.datasource.repository.EntityRepository;
import org.silverpeas.core.persistence.datasource.repository.WithSaveAndFlush;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Yohann Chastagnier
 */
public interface DelayedNotificationDataRepository
    extends EntityRepository<DelayedNotificationData>, WithSaveAndFlush<DelayedNotificationData> {

  List<Integer> findAllUsersToBeNotified(Collection<Integer> aimedChannels);

  List<DelayedNotificationData> findByUserId(int userId, Collection<Integer> aimedChannels);

  long deleteByIds(Collection<Long> ids);

  List<Integer> findUsersToBeNotified(Set<NotifChannel> aimedChannels,
      Set<DelayedNotificationFrequency> aimedFrequencies,
      boolean isThatUsersWithNoSettingHaveToBeNotified);

  List<DelayedNotificationData> findDelayedNotification(
      DelayedNotificationData delayedNotification);

}
