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
package org.silverpeas.core.security.token.persistent.repository;

import org.silverpeas.core.annotation.Repository;
import org.silverpeas.core.persistence.datasource.repository.jpa.BasicJpaEntityRepository;
import org.silverpeas.core.security.token.persistent.PersistentResourceToken;

import javax.inject.Singleton;

/**
 * @author Yohann Chastagnier
 */
@Repository
@Singleton
public class PersistentResourceTokenJPARepository
    extends BasicJpaEntityRepository<PersistentResourceToken>
    implements PersistentResourceTokenRepository {

  @Override
  public PersistentResourceToken getByTypeAndResourceId(String type, String resourceId) {
    return getFromNamedQuery("PersistentResourceToken.getByTypeAndResourceId",
        newNamedParameters().add("type", type).add("resourceId", resourceId));
  }

  @Override
  public PersistentResourceToken getByToken(String token) {
    return getFromNamedQuery("PersistentResourceToken.getByToken",
        newNamedParameters().add("token", token));
  }
}
