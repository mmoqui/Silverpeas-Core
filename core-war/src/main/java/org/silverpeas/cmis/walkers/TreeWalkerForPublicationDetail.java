/*
 * Copyright (C) 2000 - 2021 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.cmis.walkers;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.silverpeas.cmis.Filtering;
import org.silverpeas.cmis.Paging;
import org.silverpeas.cmis.util.CmisProperties;
import org.silverpeas.core.ResourceIdentifier;
import org.silverpeas.core.ResourceReference;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.annotation.Service;
import org.silverpeas.core.cmis.model.CmisFolder;
import org.silverpeas.core.cmis.model.CmisObject;
import org.silverpeas.core.cmis.model.CmisObjectFactory;
import org.silverpeas.core.cmis.model.Publication;
import org.silverpeas.core.cmis.model.TypeId;
import org.silverpeas.core.contribution.attachment.AttachmentService;
import org.silverpeas.core.contribution.attachment.model.Document;
import org.silverpeas.core.contribution.attachment.model.SimpleDocument;
import org.silverpeas.core.contribution.model.ContributionIdentifier;
import org.silverpeas.core.contribution.publication.model.Location;
import org.silverpeas.core.contribution.publication.model.PublicationDetail;
import org.silverpeas.core.contribution.publication.model.PublicationPK;
import org.silverpeas.core.contribution.publication.service.PublicationService;
import org.silverpeas.core.i18n.LocalizedResource;
import org.silverpeas.core.node.model.NodeDetail;
import org.silverpeas.core.node.model.NodePK;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link CmisObjectsTreeWalker} object that knows how to walk down the subtree rooted to a
 * publication in a given Silverpeas application. The publication is a localized contribution that
 * can have a content (form, rich text (WYSIWYG), ...) and that can have one or more documents
 * attached to it.
 * @author mmoquillon
 */
@Service
@Singleton
public class TreeWalkerForPublicationDetail extends AbstractCmisObjectsTreeWalker {

  @Inject
  private PublicationService publicationService;

  @Inject
  private AttachmentService attachmentService;

  @Override
  public ContentStream getContentStream(final String objectId, final String language,
      final long offset, final long length) {
    throw new CmisNotSupportedException("The content stream isn't supported by publications");
  }

  /**
   * The walker takes in charge both the creation of document files from the specified CMIS
   * properties and its attachment to the publication identified by the given folder identifier.
   * @param folderId the unique identifier of a publication in the CMIS objects tree.
   * @param properties the CMIS properties of the {@link SimpleDocument} to create.
   * @param contentStream a stream on the document's content. Must not be null.
   * @param language the ISO 639-1 code of the language in which the textual folder properties are
   * expressed.
   * @return the {@link org.silverpeas.core.cmis.model.DocumentFile} instance that has been created
   * and attached to the {@link Publication} object identified by the folderId parameter.
   */
  @Override
  public CmisObject createChildData(final String folderId, final CmisProperties properties,
      final ContentStream contentStream, final String language) {
    PublicationDetail publication = getSilverpeasObjectById(folderId);
    properties.setParentObjectId(folderId);
    TypeId typeId = properties.getObjectTypeId();
    AbstractCmisObjectsTreeWalker walker = getTreeWalkerSelector().selectByTypeObjectId(typeId);
    Document document = walker.createSilverpeasObject(properties, language);
    // TODO create attachment with attachmentService

    return walker.createCmisObject(document, language);
  }

  /**
   * Creates a new {@link PublicationDetail} instance from the specified CMIS properties and in
   * the given language.
   * @param properties the CMIS properties of a {@link Publication} object.
   * @param language the ISO 639-1 code of the language in which the textual properties are
   * expressed.
   * @return a {@link PublicationDetail} instance.
   */
  @Override
  @SuppressWarnings("unchecked")
  protected PublicationDetail createSilverpeasObject(
      final CmisProperties properties, final String language) {
    PublicationDetail publication = new PublicationDetail();
    publication.setPk(new PublicationPK(ResourceReference.UNKNOWN_ID));
    publication.setLanguage(language);
    publication.setName(properties.getName());
    publication.setDescription(properties.getDescription());
    publication.setCreationDate(properties.getCreationDate());
    publication.setCreatorId(User.getCurrentRequester().getId());
    publication.setUpdateDate(publication.getCreationDate());
    publication.setUpdaterId(publication.getCreatorId());
    publication.setImportance(1);
    publication.setInfoId("0");
    return publication;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected PublicationDetail getSilverpeasObjectById(final String objectId) {
    return publicationService.getDetail(asPublicationPK(objectId));
  }

  @Override
  protected Stream<LocalizedResource> getAllowedChildrenOfSilverpeasObject(
      final ResourceIdentifier parentId, final User user) {
    String language = user.getUserPreferences().getLanguage();
    ResourceReference ref = ResourceReference.to((ContributionIdentifier)parentId);
    return attachmentService.listDocumentsByForeignKey(ref, language)
        .stream()
        .map(Document::new);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Publication createCmisObject(final LocalizedResource resource, final String language) {
    PublicationDetail pub = (PublicationDetail) resource;
    ContributionIdentifier folder = getFolder(pub);
    return CmisObjectFactory.getInstance().createPublication(pub, folder, language);
  }

  @Override
  protected boolean isObjectSupported(final String objectId) {
    try {
      return ContributionIdentifier.decode(objectId)
          .getType()
          .equals(PublicationDetail.getResourceType());
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  protected boolean isTypeSupported(final TypeId typeId) {
    return typeId == TypeId.SILVERPEAS_PUBLICATION;
  }

  @Override
  protected List<ObjectInFolderContainer> browseObjectsInFolderTree(final LocalizedResource object,
      final Filtering filtering, final long depth) {
    if (filtering.getIncludeCmisObjectTypes() == Filtering.IncludeCmisObjectTypes.ONLY_FOLDERS) {
      return Collections.emptyList();
    }
    User user = filtering.getCurrentUser();
    List<LocalizedResource> children =
        getAllowedChildrenOfSilverpeasObject(object.getIdentifier(), user)
            .collect(Collectors.toList());
    return browseObjectsInFolderSubTrees(children, filtering, depth);
  }

  @Override
  protected ObjectInFolderList browseObjectsInFolder(final LocalizedResource object,
      final Filtering filtering, final Paging paging) {
    User user = filtering.getCurrentUser();
    List<LocalizedResource> children =
        getAllowedChildrenOfSilverpeasObject(object.getIdentifier(), user)
            .collect(Collectors.toList());
    return buildObjectInFolderList(children, filtering, paging);
  }

  @Override
  protected List<ObjectParentData> browseParentsOfObject(final LocalizedResource object,
      final Filtering filtering) {
    PublicationDetail pub = (PublicationDetail) object;
    String language = filtering.getLanguage();
    String folderId = getFolder(pub).asString();
    AbstractCmisObjectsTreeWalker walker = getTreeWalkerSelector().selectByObjectIdOrFail(folderId);
    LocalizedResource folder = walker.getSilverpeasObjectById(folderId);
    CmisFolder cmisParent = walker.createCmisObject(folder, language);
    CmisObject cmisObject = createCmisObject(pub, language);
    ObjectParentData parentData = buildObjectParentData(cmisParent, cmisObject, filtering);
    return Collections.singletonList(parentData);
  }

  private PublicationPK asPublicationPK(final String pubId) {
    ContributionIdentifier identifier = ContributionIdentifier.decode(pubId);
    return new PublicationPK(identifier.getLocalId(), identifier.getComponentInstanceId());
  }

  private ContributionIdentifier getFolder(final PublicationDetail publication) {
    Location location = publicationService.getMainLocation(publication.getPK())
        .orElse(new Location(NodePK.ROOT_NODE_ID, publication.getInstanceId()));
    return ContributionIdentifier.from(location, NodeDetail.TYPE);
  }
}
  