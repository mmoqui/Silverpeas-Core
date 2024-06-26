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

package org.silverpeas.core.workflow.engine.user;

import org.silverpeas.core.annotation.Repository;
import org.silverpeas.core.notification.system.ResourceEvent;
import org.silverpeas.core.persistence.Transaction;
import org.silverpeas.core.persistence.datasource.repository.jpa.NamedParameters;
import org.silverpeas.core.persistence.datasource.repository.jpa.SilverpeasJpaEntityRepository;
import org.silverpeas.kernel.logging.SilverLogger;
import org.silverpeas.core.workflow.api.user.Replacement;
import org.silverpeas.core.workflow.api.user.User;

import javax.inject.Inject;
import java.util.List;

/**
 * Implementation of the business replacement repository by using JPA.
 * This repository fires events at each modification (saving, deletion, ...) operated on the
 * replacements so that services can be hooks on such events to perform additional treatments.
 * @author mmoquillon
 */
@Repository
public class ReplacementRepository extends SilverpeasJpaEntityRepository<ReplacementImpl>
    implements Replacement.Repository {

  private static final String WORKFLOW_PARAM = "workflow";
  private static final String SUBSTITUTE_PARAM = "substitute";
  private static final String INCUMBENT_PARAM = "incumbent";

  @Inject
  private ReplacementEventNotifier notifier;

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Replacement<T>> T save(final Replacement<T> replacement) {
    final ReplacementImpl previous = Transaction.performInNew(() -> {
      if (replacement.getId() != null) {
        return findById(replacement.getId());
      }
      return null;
    });
    final ReplacementImpl saved = super.save((ReplacementImpl) replacement);
    if (previous != null) {
      notifyUsers(ResourceEvent.Type.UPDATE, previous, saved);
    } else {
      notifyUsers(ResourceEvent.Type.CREATION, saved);
    }
    return (T) saved;
  }

  @Override
  public <T extends Replacement<T>> void delete(final Replacement<T> replacement) {
    super.delete((ReplacementImpl) replacement);
    notifyUsers(ResourceEvent.Type.DELETION, replacement);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ReplacementImpl> findAllByIncumbentAndByWorkflow(final User user,
      final String workflowInstanceId) {
    NamedParameters parameters = newNamedParameters()
        .add(INCUMBENT_PARAM, user.getUserId())
        .add(WORKFLOW_PARAM, workflowInstanceId);
    return findByNamedQuery("Replacement.findAllByIncumbentAndByWorkflow", parameters);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ReplacementImpl> findAllBySubstituteAndByWorkflow(final User user,
      final String workflowInstanceId) {
    NamedParameters parameters = newNamedParameters()
        .add(SUBSTITUTE_PARAM, user.getUserId())
        .add(WORKFLOW_PARAM, workflowInstanceId);
    return findByNamedQuery("Replacement.findAllBySubstituteAndByWorkflow", parameters);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ReplacementImpl> findAllByWorkflow(final String workflowInstanceId) {
    NamedParameters parameters = newNamedParameters()
        .add(WORKFLOW_PARAM, workflowInstanceId);
    return findByNamedQuery("Replacement.findAllByWorkflow", parameters);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ReplacementImpl> findAllByUsersAndByWorkflow(final User incumbent,
      final User substitute, final String workflowInstanceId) {
    NamedParameters parameters = newNamedParameters()
        .add(INCUMBENT_PARAM, incumbent.getUserId())
        .add(SUBSTITUTE_PARAM, substitute.getUserId())
        .add(WORKFLOW_PARAM, workflowInstanceId);
    return findByNamedQuery("Replacement.findAllByUsersAndByWorkflow", parameters);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ReplacementImpl findById(final String replacementId) {
    return super.getById(replacementId);
  }

  private void notifyUsers(final ResourceEvent.Type cause, final Replacement<?>... replacements) {
    try {
      notifier.notifyEventOn(cause, replacements);
    } catch (Exception e) {
      SilverLogger.getLogger(this).error(e);
    }
  }
}
  