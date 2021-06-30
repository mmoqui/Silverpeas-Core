/*
 * Copyright (C) 2000 - 2021 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Lib
 * Open Source Software ("FLOSS") applications as described in Silverpeas
 * FLOSS exception. You should have received a copy of the text describin
 * the FLOSS exception, and it is also available here:
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public Licen
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.silverpeas.cmis.walkers;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.silverpeas.cmis.CMISEnvForTests;
import org.silverpeas.cmis.util.CmisDateConverter;
import org.silverpeas.cmis.util.CmisProperties;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.cmis.model.CmisObject;
import org.silverpeas.core.cmis.model.DocumentFile;
import org.silverpeas.core.cmis.model.TypeId;
import org.silverpeas.core.contribution.model.ContributionIdentifier;
import org.silverpeas.core.contribution.publication.model.PublicationDetail;
import org.silverpeas.core.util.MimeTypes;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests about the content stream of the objects in the CMIS tree.
 * @author mmoquillon
 */
@DisplayName("Test the creation of Silverpeas objects in the CMIS tree through the walkers")
class ObjectCreationInTreeWalkerTest extends CMISEnvForTests {

  @Test
  @DisplayName("Collaborative space doesn't support creation of spaces")
  void createSpaceInToSpace() {
    final String spaceId = "WA1";
    CmisProperties cmisProperties = new CmisProperties();
    cmisProperties.setObjectTypeId(TypeId.SILVERPEAS_SPACE)
        .setName("My space")
        .setDescription("A space")
        .setDefaultProperties()
        .setCreationData(User.getCurrentRequester().getDisplayedName(), new Date().getTime());
    CmisObjectsTreeWalker walker = CmisObjectsTreeWalker.getInstance();
    assertThrows(CmisNotSupportedException.class,
        () -> walker.createChildData(spaceId, cmisProperties, null, "fr"));
  }

  @Test
  @DisplayName("Collaborative space doesn't support creation of applications")
  void createApplicationInToSpace() {
    final String spaceId = "WA1";
    CmisProperties cmisProperties = new CmisProperties();
    cmisProperties.setObjectTypeId(TypeId.SILVERPEAS_APPLICATION)
        .setName("My app")
        .setDescription("An application")
        .setDefaultProperties()
        .setCreationData(User.getCurrentRequester().getDisplayedName(), new Date().getTime());
    CmisObjectsTreeWalker walker = CmisObjectsTreeWalker.getInstance();
    assertThrows(CmisNotSupportedException.class,
        () -> walker.createChildData(spaceId, cmisProperties, null, "fr"));
  }

  @Test
  @DisplayName("Application doesn't support the creation of folders")
  void createAFolderIntoApplication() {
    final String appId = "kmelia1";
    CmisProperties cmisProperties = new CmisProperties();
    cmisProperties.setObjectTypeId(TypeId.SILVERPEAS_FOLDER)
        .setName("My folder")
        .setDescription("A folder")
        .setDefaultProperties()
        .setCreationData(User.getCurrentRequester().getDisplayedName(), new Date().getTime());
    CmisObjectsTreeWalker walker = CmisObjectsTreeWalker.getInstance();
    assertThrows(CmisNotSupportedException.class,
        () -> walker.createChildData(appId, cmisProperties, null, "fr"));
  }

  @Test
  @DisplayName("Application supports the creation of publications")
  void createAPublicationIntoApplication() {
    final String appId = "kmelia1";
    Date now = new Date();
    CmisProperties cmisProperties = new CmisProperties();
    cmisProperties.setObjectTypeId(TypeId.SILVERPEAS_PUBLICATION)
        .setName("My publication")
        .setDescription("A publication")
        .setDefaultProperties()
        .setCreationData(User.getCurrentRequester().getDisplayedName(), now.getTime());
    CmisObjectsTreeWalker walker = CmisObjectsTreeWalker.getInstance();
    CmisObject publication = walker.createChildData(appId, cmisProperties, null, "fr");
    assertThat(publication, notNullValue());
    assertThat(publication.getBaseTypeId(), is(BaseTypeId.CMIS_FOLDER));
    assertThat(publication.getTypeId(), is(TypeId.SILVERPEAS_PUBLICATION));
    assertThat(publication.getName(), is("My publication"));
    assertThat(publication.getDescription(), is("A publication"));
    assertThat(publication.getCreator(), is(User.getCurrentRequester()
        .getDisplayedName()));

    long dateTimeInMillis = CmisDateConverter.millisToCalendar(now.getTime())
        .getTimeInMillis();
    assertThat(publication.getCreationDate(), is(dateTimeInMillis));
  }

  @Test
  @DisplayName("Publication supports the creation of documents")
  void createADocumentIntoPublication() {
    final ContributionIdentifier pubId =
        ContributionIdentifier.from("kmelia1", "1", PublicationDetail.TYPE);
    Date now = new Date();

    InputStream content = getClass().getResourceAsStream("/HistoryOfSmalltalk.pdf");
    ContentStreamImpl contentStream = new ContentStreamImpl();
    contentStream.setStream(content);
    contentStream.setMimeType(MimeTypes.PDF_MIME_TYPE);
    contentStream.setFileName("HistoryOfSmalltalk.pdf");
    contentStream.setLength(BigInteger.valueOf(889449));

    CmisProperties cmisProperties = new CmisProperties();
    cmisProperties.setObjectTypeId(TypeId.SILVERPEAS_DOCUMENT)
        .setName("History Of Smalltalk")
        .setDescription("How smalltak has been created")
        .setDefaultProperties()
        .setCreationData(User.getCurrentRequester()
            .getDisplayedName(), now.getTime())
        .setContent(null, MimeTypes.PDF_MIME_TYPE, 889449, "HistoryOfSmalltalk.pdf");
    CmisObjectsTreeWalker walker = CmisObjectsTreeWalker.getInstance();
    CmisObject object =
        walker.createChildData(pubId.asString(), cmisProperties, contentStream, "fr");
    assertThat(object, notNullValue());
    assertThat(object instanceof DocumentFile, is(true));

    DocumentFile document = (DocumentFile) object;
    assertThat(document.getBaseTypeId(), is(BaseTypeId.CMIS_DOCUMENT));
    assertThat(document.getTypeId(), is(TypeId.SILVERPEAS_DOCUMENT));
    assertThat(document.getTitle(), is("History Of Smalltalk"));
    assertThat(document.getName(), is("HistoryOfSmalltalk.pdf"));
    assertThat(document.getDescription(), is("How smalltak has been created"));
    assertThat(document.getCreator(), is(User.getCurrentRequester()
        .getDisplayedName()));

    long dateTimeInMillis = CmisDateConverter.millisToCalendar(now.getTime())
        .getTimeInMillis();
    assertThat(document.getCreationDate(), is(dateTimeInMillis));
  }
}
