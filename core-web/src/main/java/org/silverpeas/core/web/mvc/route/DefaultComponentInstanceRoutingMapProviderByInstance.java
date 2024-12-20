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

package org.silverpeas.core.web.mvc.route;

import org.silverpeas.core.annotation.Provider;
import org.silverpeas.kernel.cache.model.SimpleCache;
import org.silverpeas.core.cache.service.CacheAccessorProvider;
import org.silverpeas.kernel.util.Mutable;

import javax.annotation.Nonnull;

/**
 * @author silveryocha
 */
@Provider
class DefaultComponentInstanceRoutingMapProviderByInstance
    implements ComponentInstanceRoutingMapProviderByInstance {
  private static final String CACHE_KEY_PREFIX =
      ComponentInstanceRoutingMap.class.getName() + "###";

  @Override
  @Nonnull
  public ComponentInstanceRoutingMapProvider getByInstanceId(final String instanceId) {
    SimpleCache cache = CacheAccessorProvider.getThreadCacheAccessor().getCache();
    String cacheKey = CACHE_KEY_PREFIX + instanceId;

    final Mutable<DefaultComponentInstanceRoutingMapProvider> componentRoutingMap =
        Mutable.ofNullable(cache.get(cacheKey, DefaultComponentInstanceRoutingMapProvider.class));
    if (componentRoutingMap.isPresent()) {
      return componentRoutingMap.get();
    }

    final DefaultComponentInstanceRoutingMapProvider routingMapProvider =
        new DefaultComponentInstanceRoutingMapProvider(instanceId);
    cache.put(cacheKey, routingMapProvider);
    return routingMapProvider;
  }
}
