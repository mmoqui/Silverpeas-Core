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
package org.silverpeas.web.attachment;

import org.silverpeas.core.contribution.attachment.ActifyDocumentProcessor;
import org.silverpeas.core.contribution.attachment.AttachmentService;
import org.silverpeas.core.contribution.attachment.model.SimpleDocument;
import org.silverpeas.core.contribution.attachment.model.SimpleDocumentPK;
import org.silverpeas.core.util.file.FileRepositoryManager;
import org.silverpeas.kernel.logging.SilverLogger;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Importer of 3D documents generated by Actify (from a CAD document) in Silverpeas.
 */
public class ActifyToAttachment extends HttpServlet {

  private static final long serialVersionUID = -1903790800260389933L;

  @Inject
  private AttachmentService attachmentService;

  /**
   * Updates an existing attachment in Silverpeas with the 3D document generated by Actify for that
   * attachment.
   *
   * @param req the incoming HTTP request with as parameter the file size, the identifier of an
   * existing attachment, and the name of the file resulting of the conversion.
   * @param res the sent back HTTP response.
   */
  @Override
  public void service(HttpServletRequest req, HttpServletResponse res) {
    if (ActifyDocumentProcessor.isActifySupportEnabled()) {
      try {

        long size = Long.parseLong(req.getParameter("FileSize"));
        String attachmentId = req.getParameter("AttachmentId");
        String logicalName = req.getParameter("LogicalName");
        boolean indexIt = false;
        // Get AttachmentDetail Object
        SimpleDocument ad = attachmentService.searchDocumentById(
            new SimpleDocumentPK(attachmentId), null);
        if (ad != null) {
          // Remove string "HIDDEN" from the beginning of the instanceId
          String instanceId = ad.getInstanceId().substring(7);
          ad.getPk().setComponentName(instanceId);
          ad.setSize(size);
          ad.setFilename(logicalName);
          attachmentService.updateAttachment(ad, indexIt, false);
          // Copy 3D file converted from Actify Work directory to Silverpeas workspaces
          String actifyWorkingPath = ActifyDocumentProcessor.getActifyResultPath();
          String srcFile = actifyWorkingPath + File.separator + logicalName;
          String destFile = ad.getAttachmentPath();

          FileRepositoryManager.copyFile(srcFile, destFile);
        }
      } catch (Exception e) {
        SilverLogger.getLogger(this).error(e);
      }
    }
  }
}
