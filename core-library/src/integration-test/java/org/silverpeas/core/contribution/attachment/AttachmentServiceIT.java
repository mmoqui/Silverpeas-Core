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
package org.silverpeas.core.contribution.attachment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.silverpeas.core.ResourceReference;
import org.silverpeas.core.admin.user.model.SilverpeasRole;
import org.silverpeas.core.contribution.attachment.model.DocumentType;
import org.silverpeas.core.contribution.attachment.model.SimpleAttachment;
import org.silverpeas.core.contribution.attachment.model.SimpleDocument;
import org.silverpeas.core.contribution.attachment.model.SimpleDocumentPK;
import org.silverpeas.core.contribution.attachment.repository.DocumentRepository;
import org.silverpeas.core.contribution.attachment.repository.SimpleDocumentMatcher;
import org.silverpeas.core.test.WarBuilder4LibCore;
import org.silverpeas.core.test.jcr.JcrIntegrationIT;
import org.silverpeas.core.test.util.RandomGenerator;
import org.silverpeas.core.util.Charsets;
import org.silverpeas.core.util.DateUtil;
import org.silverpeas.core.util.MimeTypes;
import org.silverpeas.kernel.util.Pair;
import org.silverpeas.core.jcr.JCRSession;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author ehugonnet
 */
@RunWith(Arquillian.class)
public class AttachmentServiceIT extends JcrIntegrationIT {

  private static final String instanceId = "kmelia974";
  private static final String foreignInstanceId = "kmelia38";
  private SimpleDocumentPK existingFrDoc;
  private SimpleDocumentPK existingEnDoc;

  @Inject
  private AttachmentService instance;

  @Deployment
  public static Archive<?> createTestArchive() {
    return WarBuilder4LibCore.onWarForTestClass(AttachmentServiceIT.class)
        .addJcrFeatures()
        .addPublicationTemplateFeatures()
        .testFocusedOn(war -> war.addAsResource("LibreOffice.odt"))
        .build();
  }

  @Before
  public void setUpJcr() throws Exception {
    try (JCRSession session = JCRSession.openSystemSession()) {
      DocumentRepository documentRepository = new DocumentRepository();
      if (!session.getRootNode()
          .hasNode(instanceId)) {
        session.getRootNode()
            .addNode(instanceId, NodeType.NT_FOLDER);
      }
      Date creationDate = RandomGenerator.getRandomCalendar()
          .getTime();
      SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
      String foreignId = "node18";
      SimpleAttachment attachment = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
      InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      existingFrDoc = new DocumentRepository().createDocument(session, document);
      document.setPK(existingFrDoc);
      documentRepository.storeContent(document, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      foreignId = "node19";
      attachment = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      document = new SimpleDocument(emptyId, foreignId, 0, false, attachment);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      existingEnDoc = documentRepository.createDocument(session, document);
      document.setPK(existingEnDoc);
      documentRepository.storeContent(document, content);
      session.save();
    }
  }

  private NodeResult getComponentJcrNode(String... pathes) {
    List<String> pathParts = new ArrayList<>();
    pathParts.add(instanceId);
    Collections.addAll(pathParts, pathes);
    return getJcrNode(pathParts.toArray(new String[0]));
  }

  private NodeResult getJcrNode(String... pathes) {
    Node node;
    try (JCRSession session = JCRSession.openSystemSession()) {
      node = session.getNode('/' + StringUtils.join(pathes, '/'));
      return new NodeResult(node.getPath(), node.getNodes().getSize());
    } catch (PathNotFoundException e) {
      // Nothing to do, the root node doesn't exist. That is all.
      return null;
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  private static class NodeResult {
    private final String path;

    private final long nbChildren;

    private NodeResult(final String path, final long nbChildren) {
      this.path = path;
      this.nbChildren = nbChildren;
    }

    public long getNbChildren() {
      return nbChildren;
    }

    public String getPath() {
      return path;
    }
  }

  /**
   * Test of addContent method, of class AttachmentService.
   */
  @Test
  public void updateStreamContent() {
    String currentLang = "fr";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, currentLang);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("Ceci est un test"));
    InputStream content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    instance.updateAttachment(document, content, false, false);
    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("This is a test"));
  }

  /**
   * Test of addContent method, of class AttachmentService.
   */
  @Test
  public void addNewStreamContent() {
    String currentLang = "fr";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, currentLang);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("Ceci est un test"));
    currentLang = "en";
    InputStream content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    document.setLanguage(currentLang);
    instance.updateAttachment(document, content, false, false);
    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("This is a test"));
    currentLang = "fr";
    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("Ceci est un test"));
  }

  /**
   * Test of addContent method, of class AttachmentService.
   */
  @Test
  public void addFileContent() throws IOException {
    File file = getDocumentNamed("/LibreOffice.odt");
    String currentLang = "fr";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, currentLang);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("Ceci est un test"));
    currentLang = "en";
    document.setLanguage(currentLang);
    instance.updateAttachment(document, file, false, false);
    File tempFile = File.createTempFile("LibreOffice", ".odt");
    instance.getBinaryContent(tempFile, existingFrDoc, currentLang);
    assertThat(FileUtils.contentEquals(file, tempFile), is(true));
    currentLang = "fr";
    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, existingFrDoc, currentLang);
    assertThat(out.toString(Charsets.UTF_8), is("Ceci est un test"));
  }

  /**
   * Test of addContent method, of class AttachmentService.
   */
  @Test
  public void updateFileContent() throws IOException {
    File file = getDocumentNamed("/LibreOffice.odt");
    String currentLang = "fr";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, currentLang);
    instance.updateAttachment(document, file, false, false);
    File tempFile = File.createTempFile("LibreOffice", ".odt");
    instance.getBinaryContent(tempFile, existingFrDoc, currentLang);
    assertThat(FileUtils.contentEquals(file, tempFile), is(true));
  }

  /**
   * Test of getBinaryContent method, of class AttachmentService.
   */
  @Test
  public void getBinaryContentIntoFile() throws IOException {
    File file = File.createTempFile("AttachmentServiceTest", "docx");
    SimpleDocumentPK pk = existingEnDoc;
    String lang = "en";
    instance.getBinaryContent(file, pk, lang);
    assertThat(file.exists(), is(true));
    assertThat(file.isFile(), is(true));
    assertThat(file.length(), is((long) "This is a test".getBytes(Charsets.UTF_8).length));
  }

  /**
   * Test of getBinaryContent method, of class AttachmentService.
   */
  @Test
  public void getBinaryContentIntoOutputStream() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SimpleDocumentPK pk = existingEnDoc;
    String lang = "en";
    instance.getBinaryContent(out, pk, lang);
    assertThat(out, is(notNullValue()));
    byte[] content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is("This is a test".getBytes(Charsets.UTF_8).length));
    assertThat(new String(content, Charsets.UTF_8), is("This is a test"));

    out = new ByteArrayOutputStream();
    lang = "fr";
    instance.getBinaryContent(out, pk, lang);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(14));
    assertThat(new String(content, Charsets.UTF_8), is("This is a test"));

    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, 0, -1);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(14));
    assertThat(new String(content, Charsets.UTF_8), is("This is a test"));

    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, 0, 0);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(0));
    assertThat(new String(content, Charsets.UTF_8), is(""));

    long length = "This".length();
    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, -10, length);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(4));
    assertThat(new String(content, Charsets.UTF_8), is("This"));

    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, 0, length);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(4));
    assertThat(new String(content, Charsets.UTF_8), is("This"));

    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, 5, length);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(4));
    assertThat(new String(content, Charsets.UTF_8), is("is a"));

    out = new ByteArrayOutputStream();
    instance.getBinaryContent(out, pk, lang, 0, 500000000);
    assertThat(out, is(notNullValue()));
    content = out.toByteArray();
    assertThat(content, is(notNullValue()));
    assertThat(content.length, is(14));
    assertThat(new String(content, Charsets.UTF_8), is("This is a test"));
  }

  /**
   * Test of addXmlForm method, of class AttachmentService.
   */
  @Test
  public void addXmlForm() {
    String language = "fr";
    String xmlFormName = "15";
    SimpleDocument result = instance.searchDocumentById(existingFrDoc, language);
    assertThat(result, is(notNullValue()));
    assertThat(result.getXmlFormId(), is("5"));
    instance.addXmlForm(existingFrDoc, language, xmlFormName);
    result = instance.searchDocumentById(existingFrDoc, language);
    assertThat(result, is(notNullValue()));
    assertThat(result.getXmlFormId(), is(xmlFormName));
    instance.addXmlForm(existingFrDoc, language, null);
    result = instance.searchDocumentById(existingFrDoc, language);
    assertThat(result, is(notNullValue()));
    assertThat(result.getXmlFormId(), is(nullValue()));
  }

  /**
   * Test of cloneDocument method, of class AttachmentService.
   */
  @Test
  public void cloneDocument() throws IOException {
    String language = "fr";
    String foreignCloneId = "node59";
    SimpleDocument original = instance.searchDocumentById(existingFrDoc, language);
    SimpleDocumentPK clonePk = instance.cloneDocument(original, foreignCloneId);
    SimpleDocument clone = instance.searchDocumentById(clonePk, language);
    original.setCloneId(original.getId());
    SimpleDocument updatedOriginal = instance.searchDocumentById(existingFrDoc, language);
    assertThat(updatedOriginal, SimpleDocumentMatcher.matches(original));
    original.setCloneId(null);
    original.setForeignId(foreignCloneId);
    original.setPK(clonePk);
    assertThat(clone, SimpleDocumentMatcher.matches(original));
    assertThat(FileUtils.contentEquals(new File(original.getAttachmentPath()), new File(clone.
        getAttachmentPath())), is(true));
  }

  /**
   * Test of cloneDocument method, of class AttachmentService.
   */
  @Test
  public void moveDocument() {
    String language = "fr";
    String foreignId = "73";
    SimpleDocument original = instance.searchDocumentById(existingFrDoc, language);
    File originalContent = new File(original.getAttachmentPath());
    assertThat(originalContent.exists(), is(true));
    SimpleDocumentPK pk = instance.moveDocument(original, new ResourceReference(foreignId,
        foreignInstanceId));
    SimpleDocument movedDocument = instance.searchDocumentById(pk, language);
    original.setForeignId(foreignId);
    original.setPK(pk);
    assertThat(movedDocument, SimpleDocumentMatcher.matches(original));
    assertThat(originalContent.exists(), is(false));
    File movedContent = new File(movedDocument.getAttachmentPath());
    assertThat(movedContent.exists(), is(true));
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createAttachmentFromInputStream() {
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
    SimpleDocument result = instance.createAttachment(document, content);
    assertThat(result, is(notNullValue()));
    checkFrenchSimpleDocument(result);
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createIndexedAttachmentFromInputStream() {
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
    SimpleDocument result = instance.createAttachment(document, content, true);
    assertThat(result, is(notNullValue()));
    checkFrenchSimpleDocument(result);
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createAttachmentFromInputStreamWithCallback() {
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
    SimpleDocument result = instance.createAttachment(document, content, true, true);
    assertThat(result, is(notNullValue()));
    checkFrenchSimpleDocument(result);
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createAttachmentIndexedCallbackFromFile() {
    File file = getDocumentNamed("/LibreOffice.odt");
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize(file.length())
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    SimpleDocument result = instance.createAttachment(document, file, true, true);
    assertThat(result, is(notNullValue()));
    checkFrenchFileSimpleDocument(result);
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createAttachmentNotIndexedFromFile() {
    File file = getDocumentNamed("/LibreOffice.odt");
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize(file.length())
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    SimpleDocument result = instance.createAttachment(document, file, false);
    assertThat(result, is(notNullValue()));
    checkFrenchFileSimpleDocument(result);
  }

  /**
   * Test of createAttachment method, of class AttachmentService.
   */
  @Test
  public void createAttachmentFromFile() {
    File file = getDocumentNamed("/LibreOffice.odt");
    Date creationDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    SimpleAttachment attachment = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize(file.length())
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", creationDate)
        .setFormId("5")
        .build();
    SimpleDocument document = new SimpleDocument(emptyId, foreignId, 10, false, attachment);
    SimpleDocument result = instance.createAttachment(document, file);
    assertThat(result, is(notNullValue()));
    checkFrenchFileSimpleDocument(result);
  }

  /**
   * Test of deleteAttachment method, of class AttachmentService.
   */
  @Test
  public void deleteAllDocuments() {
    assertThat(getComponentJcrNode(), notNullValue());
    SimpleDocument documentFr = instance.searchDocumentById(existingFrDoc, null);
    SimpleDocument documentEn = instance.searchDocumentById(existingEnDoc, null);
    assertThat(documentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    instance.deleteAllAttachments(instanceId);
    documentFr = instance.searchDocumentById(existingFrDoc, null);
    documentEn = instance.searchDocumentById(existingEnDoc, null);
    assertThat(documentFr, nullValue());
    assertThat(documentEn, nullValue());
    assertThat(getComponentJcrNode(), nullValue());
  }

  /**
   * Test of deleteAllAttachments method, of class AttachmentService.
   */
  @Test
  public void deleteAllAttachments() {
    File file = getDocumentNamed("/LibreOffice.odt");
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s"), nullValue());
    SimpleDocument newDocumentSameResourceDocumentFr =
        instance.searchDocumentById(existingFrDoc, null);
    newDocumentSameResourceDocumentFr.setNodeName(null);
    newDocumentSameResourceDocumentFr.setOldSilverpeasId(0);
    newDocumentSameResourceDocumentFr.setDocumentType(DocumentType.form);
    newDocumentSameResourceDocumentFr.setTitle("DELETE_ALL_TEST");
    SimpleDocument otherDocumentSameResourceDocumentFr =
        instance.createAttachment(newDocumentSameResourceDocumentFr, file);
    SimpleDocument documentFr = instance.searchDocumentById(existingFrDoc, null);
    SimpleDocument documentEn = instance.searchDocumentById(existingEnDoc, null);
    assertThat(documentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr,
        not(sameInstance(newDocumentSameResourceDocumentFr)));
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));

    instance.deleteAllAttachments(documentFr.getForeignId(), documentFr.getInstanceId());

    documentFr = instance.searchDocumentById(existingFrDoc, null);
    documentEn = instance.searchDocumentById(existingEnDoc, null);
    otherDocumentSameResourceDocumentFr =
        instance.searchDocumentById(otherDocumentSameResourceDocumentFr.getPk(), null);
    assertThat(documentFr, nullValue());
    assertThat(otherDocumentSameResourceDocumentFr, nullValue());
    assertThat(documentEn, notNullValue());
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(1L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(0L));
  }

  /**
   * Test of copyAllAttachments method, of class AttachmentService.
   */
  @Test
  public void copyAllAttachments() {
    File file = getDocumentNamed("/LibreOffice.odt");
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s"), nullValue());
    SimpleDocument newDocumentSameResourceDocumentFr =
        instance.searchDocumentById(existingFrDoc, null);
    newDocumentSameResourceDocumentFr.setId(null);
    newDocumentSameResourceDocumentFr.setNodeName(null);
    newDocumentSameResourceDocumentFr.setOldSilverpeasId(0);
    newDocumentSameResourceDocumentFr.setDocumentType(DocumentType.form);
    newDocumentSameResourceDocumentFr.setTitle("DELETE_ALL_TEST");
    SimpleDocument otherDocumentSameResourceDocumentFr =
        instance.createAttachment(newDocumentSameResourceDocumentFr, file);
    SimpleDocument documentFr = instance.searchDocumentById(existingFrDoc, null);
    SimpleDocument documentEn = instance.searchDocumentById(existingEnDoc, null);
    assertThat(documentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr,
        not(sameInstance(newDocumentSameResourceDocumentFr)));
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));
    assertThat(getJcrNode(foreignInstanceId), nullValue());

    ResourceReference newResourcePK = new ResourceReference("newResourcePK", foreignInstanceId);
    List<Pair<SimpleDocumentPK, SimpleDocumentPK>> copiedDocumentPks = instance
        .copyAllDocuments(new ResourceReference(documentFr.getForeignId(), documentFr.getInstanceId()),
            newResourcePK);

    assertThat(copiedDocumentPks, hasSize(2));
    for (Pair<SimpleDocumentPK, SimpleDocumentPK> copy : copiedDocumentPks) {
      SimpleDocumentPK copiedDocumentPK = copy.getSecond();
      SimpleDocument copiedDocument = instance.searchDocumentById(copiedDocumentPK, "de");
      assertThat(copiedDocument, notNullValue());
      assertThat(copiedDocument.getInstanceId(), is(foreignInstanceId));
      assertThat(copiedDocument.getForeignId(), is("newResourcePK"));
      assertThat(copiedDocument.getId(), not(is(oneOf(documentEn.getId(), documentEn.getId(),
          otherDocumentSameResourceDocumentFr.getId()))));
      assertThat(copiedDocument.getOldSilverpeasId(),
          greaterThan(otherDocumentSameResourceDocumentFr.getOldSilverpeasId()));
    }
    documentFr = instance.searchDocumentById(existingFrDoc, null);
    documentEn = instance.searchDocumentById(existingEnDoc, null);
    otherDocumentSameResourceDocumentFr =
        instance.searchDocumentById(otherDocumentSameResourceDocumentFr.getPk(), null);
    assertThat(documentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));
    assertThat(getJcrNode(foreignInstanceId), notNullValue());
    assertThat(getJcrNode(foreignInstanceId, DocumentType.attachment.name() + "s").getNbChildren(),
        is(1L));
    assertThat(getJcrNode(foreignInstanceId, DocumentType.form.name() + "s").getNbChildren(),
        is(1L));
  }

  /**
   * Test of moveAllAttachments method, of class AttachmentService.
   */
  @Test
  public void moveAllAttachments() {
    File file = getDocumentNamed("/LibreOffice.odt");
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s"), nullValue());
    SimpleDocument newDocumentSameResourceDocumentFr =
        instance.searchDocumentById(existingFrDoc, null);
    newDocumentSameResourceDocumentFr.setId(null);
    newDocumentSameResourceDocumentFr.setNodeName(null);
    newDocumentSameResourceDocumentFr.setOldSilverpeasId(0);
    newDocumentSameResourceDocumentFr.setDocumentType(DocumentType.form);
    newDocumentSameResourceDocumentFr.setTitle("DELETE_ALL_TEST");
    SimpleDocument otherDocumentSameResourceDocumentFr =
        instance.createAttachment(newDocumentSameResourceDocumentFr, file);
    SimpleDocument documentEn = instance.searchDocumentById(existingEnDoc, null);
    SimpleDocument documentFr = instance.searchDocumentById(existingFrDoc, null);
    assertThat(documentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr,
        not(sameInstance(newDocumentSameResourceDocumentFr)));
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));
    assertThat(getJcrNode(foreignInstanceId), nullValue());

    ResourceReference newResourcePK = new ResourceReference("newResourcePK", instanceId);
    List<SimpleDocumentPK> movedDocumentPks = instance
        .moveAllDocuments(new ResourceReference(documentFr.getForeignId(), documentFr.getInstanceId()),
            newResourcePK);

    assertThat(movedDocumentPks, hasSize(2));
    for (SimpleDocumentPK movedDocumentPK : movedDocumentPks) {
      SimpleDocument movedDocument = instance.searchDocumentById(movedDocumentPK, "de");
      assertThat(movedDocument, notNullValue());
      assertThat(movedDocument.getInstanceId(), is(instanceId));
      assertThat(movedDocument.getForeignId(), is("newResourcePK"));
      assertThat(movedDocument.getId(),
          is(oneOf(documentFr.getId(), otherDocumentSameResourceDocumentFr.getId())));
      assertThat(movedDocument.getOldSilverpeasId(),
          lessThanOrEqualTo(otherDocumentSameResourceDocumentFr.getOldSilverpeasId()));
    }
    documentFr = instance.searchDocumentById(existingFrDoc, null);
    documentEn = instance.searchDocumentById(existingEnDoc, null);
    otherDocumentSameResourceDocumentFr =
        instance.searchDocumentById(otherDocumentSameResourceDocumentFr.getPk(), null);
    assertThat(documentFr, notNullValue());
    assertThat(documentFr.getForeignId(), is("newResourcePK"));
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr.getForeignId(), is("newResourcePK"));
    assertThat(documentEn, notNullValue());
    assertThat(documentEn.getForeignId(), not(is("newResourcePK")));
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));
    assertThat(getJcrNode(foreignInstanceId), nullValue());
  }

  /**
   * Test of moveAllAttachments method, of class AttachmentService.
   */
  @Test
  public void moveAllAttachmentsIntoAnotherComponentId() {
    File file = getDocumentNamed("/LibreOffice.odt");
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s"), nullValue());
    SimpleDocument newDocumentSameResourceDocumentFr =
        instance.searchDocumentById(existingFrDoc, null);
    newDocumentSameResourceDocumentFr.setId(null);
    newDocumentSameResourceDocumentFr.setNodeName(null);
    newDocumentSameResourceDocumentFr.setOldSilverpeasId(0);
    newDocumentSameResourceDocumentFr.setDocumentType(DocumentType.form);
    newDocumentSameResourceDocumentFr.setTitle("DELETE_ALL_TEST");
    SimpleDocument otherDocumentSameResourceDocumentFr =
        instance.createAttachment(newDocumentSameResourceDocumentFr, file);
    SimpleDocument documentEn = instance.searchDocumentById(existingEnDoc, null);
    SimpleDocument documentFr = instance.searchDocumentById(existingFrDoc, null);
    assertThat(documentFr, notNullValue());
    assertThat(documentEn, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr,
        not(sameInstance(newDocumentSameResourceDocumentFr)));
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(2L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(1L));
    assertThat(getJcrNode(foreignInstanceId), nullValue());

    ResourceReference newResourcePK = new ResourceReference("newResourcePK", foreignInstanceId);
    List<SimpleDocumentPK> movedDocumentPks = instance
        .moveAllDocuments(new ResourceReference(documentFr.getForeignId(), documentFr.getInstanceId()),
            newResourcePK);

    assertThat(movedDocumentPks, hasSize(2));
    for (SimpleDocumentPK movedDocumentPK : movedDocumentPks) {
      SimpleDocument movedDocument = instance.searchDocumentById(movedDocumentPK, "de");
      assertThat(movedDocument, notNullValue());
      assertThat(movedDocument.getInstanceId(), is(foreignInstanceId));
      assertThat(movedDocument.getForeignId(), is("newResourcePK"));
      assertThat(movedDocument.getId(),
          is(oneOf(documentFr.getId(), otherDocumentSameResourceDocumentFr.getId())));
      assertThat(movedDocument.getOldSilverpeasId(),
          lessThanOrEqualTo(otherDocumentSameResourceDocumentFr.getOldSilverpeasId()));
    }
    documentFr = instance.searchDocumentById(existingFrDoc, null);
    documentEn = instance.searchDocumentById(existingEnDoc, null);
    otherDocumentSameResourceDocumentFr =
        instance.searchDocumentById(otherDocumentSameResourceDocumentFr.getPk(), null);
    assertThat(documentFr, notNullValue());
    assertThat(documentFr.getForeignId(), is("newResourcePK"));
    assertThat(documentFr.getInstanceId(), is(foreignInstanceId));
    assertThat(otherDocumentSameResourceDocumentFr, notNullValue());
    assertThat(otherDocumentSameResourceDocumentFr.getForeignId(), is("newResourcePK"));
    assertThat(otherDocumentSameResourceDocumentFr.getInstanceId(), is(foreignInstanceId));
    assertThat(documentEn, notNullValue());
    assertThat(documentEn.getForeignId(), not(is("newResourcePK")));
    assertThat(documentEn.getInstanceId(), is(instanceId));
    assertThat(getComponentJcrNode(), notNullValue());
    assertThat(getComponentJcrNode(DocumentType.attachment.name() + "s").getNbChildren(), is(1L));
    assertThat(getComponentJcrNode(DocumentType.form.name() + "s").getNbChildren(), is(0L));
    assertThat(getJcrNode(foreignInstanceId), notNullValue());
    assertThat(getJcrNode(foreignInstanceId, DocumentType.attachment.name() + "s").getNbChildren(),
        is(1L));
    assertThat(getJcrNode(foreignInstanceId, DocumentType.form.name() + "s").getNbChildren(),
        is(1L));
  }

  /**
   * Test of deleteAttachment method, of class AttachmentService.
   */
  @Test
  public void deleteAttachment() {
    String lang = "en";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, lang);
    assertThat(document, is(notNullValue()));
    checkFrenchSimpleDocument(document);
    instance.deleteAttachment(document);
    document = instance.searchDocumentById(existingFrDoc, lang);
    assertThat(document, is(nullValue()));
  }

  /**
   * Test of deleteAttachment method, of class AttachmentService.
   */
  @Test
  public void deleteIndexedAttachment() {
    String lang = "en";
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, lang);
    assertThat(document, is(notNullValue()));
    checkFrenchSimpleDocument(document);
    instance.deleteAttachment(document, true);
    document = instance.searchDocumentById(existingFrDoc, lang);
    assertThat(document, is(nullValue()));
  }

  /**
   * Test of removeContent method, of class AttachmentService.
   */
  @Test
  public void removeContent() {
    SimpleDocument document = instance.searchDocumentById(existingFrDoc, "fr");
    checkFrenchSimpleDocument(document);
    File pathToVerify = new File(document.getAttachmentPath()).getParentFile();
    assertThat(pathToVerify.exists(), is(true));
    instance.removeContent(document, "fr", false);
    document = instance.searchDocumentById(existingFrDoc, "fr");
    assertThat(document, is(nullValue()));
    assertThat(pathToVerify.exists(), is(false));
    assertThat(pathToVerify.getParentFile().getParentFile().exists(), is(false));
    assertThat(pathToVerify.getParentFile().getParentFile().getParentFile().exists(), is(true));

    document = instance.searchDocumentById(existingEnDoc, "en");
    checkEnglishSimpleDocument(document);
    instance.removeContent(document, "fr", false);
    document = instance.searchDocumentById(existingEnDoc, "en");
    assertThat(document, is(notNullValue()));
    checkEnglishSimpleDocument(document);
  }

  @Test
  public void reorderAttachmentsAndCreateAttachment() throws RepositoryException, IOException {
    ResourceReference foreignKey = new ResourceReference("node36", instanceId);
    try (JCRSession session = JCRSession.openSystemSession()) {
      DocumentRepository documentRepository = new DocumentRepository();
      Date creationDate = RandomGenerator.getRandomCalendar()
          .getTime();
      SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
      String foreignId = foreignKey.getId();
      SimpleAttachment attachment1 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 1")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document1 = new SimpleDocument(emptyId, foreignId, 10, false, attachment1);
      InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      SimpleDocumentPK id = documentRepository.createDocument(session, document1);
      document1.setPK(id);
      documentRepository.storeContent(document1, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 2")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document2 = new SimpleDocument(emptyId, foreignId, 5, false, attachment2);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document2);
      document2.setPK(id);
      documentRepository.storeContent(document2, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment3 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 3")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document3 = new SimpleDocument(emptyId, foreignId, 100, false, attachment3);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document3);
      document3.setPK(id);
      documentRepository.storeContent(document3, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment4 = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document 4")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      SimpleDocument document4 = new SimpleDocument(emptyId, "node49", 0, false, attachment4);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document4);
      document4.setPK(id);
      documentRepository.storeContent(document4, content);
      session.save();
      List<SimpleDocument> result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(3));
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document1));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document3));
      // manual sorting
      List<SimpleDocumentPK> reorderedList = new ArrayList<>(3);
      reorderedList.add(document1.getPk());
      reorderedList.add(document2.getPk());
      reorderedList.add(document3.getPk());
      instance.reorderAttachments(reorderedList);
      result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(3));
      document1.setOrder(1);
      document2.setOrder(2);
      document3.setOrder(3);
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document1));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document3));
      // Create new document
      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment5 = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document 5")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      SimpleDocument document5 = new SimpleDocument(emptyId, foreignId, 0, false, attachment5);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document5);
      document5.setPK(id);
      documentRepository.storeContent(document5, content);
      session.save();
      document5 = documentRepository.findDocumentById(session, document5.getPk(), "fr");
      assertThat(document5.getOrder(), is(4));
      // Getting default sorting according to UI
      reorderedList = new ArrayList<>(3);
      reorderedList.add(document2.getPk());
      reorderedList.add(document1.getPk());
      reorderedList.add(document3.getPk());
      reorderedList.add(document5.getPk());
      instance.reorderAttachments(reorderedList);
      result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(4));
      document2.setOrder(1);
      document1.setOrder(2);
      document3.setOrder(3);
      document5.setOrder(4);
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document1));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document3));
      assertThat(result.get(3), SimpleDocumentMatcher.matches(document5));
    }
  }

  @Test
  public void reorderAttachmentsAndCreateAttachmentWhenSortedFromYoungestToOldestOnUI()
      throws RepositoryException, IOException {
    attachmentSettings.put("attachment.list.order", "-1");
    ResourceReference foreignKey = new ResourceReference("node36", instanceId);
    try (JCRSession session = JCRSession.openSystemSession()) {
      DocumentRepository documentRepository = new DocumentRepository();
      Date creationDate = RandomGenerator.getRandomCalendar()
          .getTime();
      SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
      String foreignId = foreignKey.getId();
      SimpleAttachment attachment1 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 1")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document1 = new SimpleDocument(emptyId, foreignId, 5, false, attachment1);
      InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      SimpleDocumentPK id = documentRepository.createDocument(session, document1);
      document1.setPK(id);
      documentRepository.storeContent(document1, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 2")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document2 = new SimpleDocument(emptyId, foreignId, 50, false, attachment2);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document2);
      document2.setPK(id);
      documentRepository.storeContent(document2, content);

      SimpleAttachment attachment3 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 3")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleDocument document3 = new SimpleDocument(emptyId, foreignId, 100, false, attachment3);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document3);
      document3.setPK(id);
      documentRepository.storeContent(document3, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment4 = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document 4")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      SimpleDocument document4 = new SimpleDocument(emptyId, "node49", 0, false, attachment4);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document4);
      document4.setPK(id);
      documentRepository.storeContent(document4, content);
      session.save();
      List<SimpleDocument> result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(3));
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document3));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document1));
      // manual sorting
      List<SimpleDocumentPK> reorderedList = new ArrayList<>(3);
      reorderedList.add(document3.getPk());
      reorderedList.add(document1.getPk());
      reorderedList.add(document2.getPk());
      instance.reorderAttachments(reorderedList);
      result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(3));
      document3.setOrder(200000);
      document1.setOrder(200001);
      document2.setOrder(200002);
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document3));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document1));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document2));
      // Create new document
      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment5 = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document 5")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      SimpleDocument document5 = new SimpleDocument(emptyId, foreignId, 0, false, attachment5);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document5);
      document5.setPK(id);
      documentRepository.storeContent(document5, content);
      session.save();
      document5 = documentRepository.findDocumentById(session, document5.getPk(), null);
      assertThat(document5.getOrder(), is(199999));
      // Getting default sorting according to UI
      reorderedList = new ArrayList<>(4);
      reorderedList.add(document5.getPk());
      reorderedList.add(document3.getPk());
      reorderedList.add(document2.getPk());
      reorderedList.add(document1.getPk());
      instance.reorderAttachments(reorderedList);
      result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(4));
      document5.setOrder(4);
      document3.setOrder(3);
      document2.setOrder(2);
      document1.setOrder(1);
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document5));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document3));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(3), SimpleDocumentMatcher.matches(document1));
    }
  }

  @Test
  public void searchAttachmentById() {
    SimpleDocument result = instance.searchDocumentById(existingFrDoc, null);
    checkFrenchSimpleDocument(result);
    assertThat(existingFrDoc.getOldSilverpeasId(), greaterThan(0L));
    String id = existingFrDoc.getId();
    existingFrDoc.setId(null);
    result = instance.searchDocumentById(existingFrDoc, null);
    checkFrenchSimpleDocument(result);
    existingFrDoc.setId(id);
    existingFrDoc.setOldSilverpeasId(-1L);
    result = instance.searchDocumentById(existingFrDoc, null);
    checkFrenchSimpleDocument(result);
  }

  @Test
  public void searchAttachmentsByExternalObject() throws RepositoryException, IOException {
    ResourceReference foreignKey = new ResourceReference("node36", instanceId);
    try (JCRSession session = JCRSession.openSystemSession()) {
      DocumentRepository documentRepository = new DocumentRepository();
      Date creationDate = RandomGenerator.getRandomCalendar()
          .getTime();
      SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
      String foreignId = foreignKey.getId();
      SimpleAttachment attachment1 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 1")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document1 = new SimpleDocument(emptyId, foreignId, 10, false, attachment1);
      InputStream content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      SimpleDocumentPK id = documentRepository.createDocument(session, document1);
      document1.setPK(id);
      documentRepository.storeContent(document1, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 2")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document2 = new SimpleDocument(emptyId, foreignId, 5, false, attachment2);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document2);
      document2.setPK(id);
      documentRepository.storeContent(document2, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      SimpleAttachment attachment3 = SimpleAttachment.builder("fr")
          .setFilename("test.odp")
          .setTitle("Mon document de test 3")
          .setDescription("Ceci est un document de test")
          .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
          .setCreationData("10", creationDate)
          .setFormId("5")
          .build();
      SimpleDocument document3 = new SimpleDocument(emptyId, foreignId, 100, false, attachment3);
      content = new ByteArrayInputStream("Ceci est un test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document3);
      document3.setPK(id);
      documentRepository.storeContent(document3, content);

      emptyId = new SimpleDocumentPK("-1", instanceId);
      foreignId = "node49";
      SimpleAttachment attachment4 = SimpleAttachment.builder("en")
          .setFilename("test.docx")
          .setTitle("My test document 4")
          .setDescription("This is a test document")
          .setSize("This is a test".getBytes(Charsets.UTF_8).length)
          .setContentType(MimeTypes.WORD_2007_MIME_TYPE)
          .setCreationData("0", creationDate)
          .setFormId("18")
          .build();
      SimpleDocument document4 = new SimpleDocument(emptyId, foreignId, 0, false, attachment4);
      content = new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
      id = documentRepository.createDocument(session, document4);
      document4.setPK(id);
      documentRepository.storeContent(document4, content);

      session.save();
      List<SimpleDocument> result = instance.listDocumentsByForeignKey(foreignKey, "fr");
      assertThat(result, is(notNullValue()));
      assertThat(result, hasSize(3));
      assertThat(result.get(0), SimpleDocumentMatcher.matches(document2));
      assertThat(result.get(1), SimpleDocumentMatcher.matches(document1));
      assertThat(result.get(2), SimpleDocumentMatcher.matches(document3));
    }
  }

  @Test
  public void updateAttachment() {
    SimpleDocument result = instance.searchDocumentById(existingFrDoc, null);
    checkFrenchSimpleDocument(result);
    Date alertDate = RandomGenerator.getRandomCalendar().getTime();
    result.setAlert(alertDate);
    result.setContentType(MimeTypes.BZ2_ARCHIVE_MIME_TYPE);
    Date creationDate = RandomGenerator.getRandomCalendar().getTime();
    result.setLastUpdateDate(creationDate);
    String creatorId = "150";
    result.setUpdatedBy(creatorId);
    String description = "Ceci est mon document de test mis à jour";
    result.setDescription(description);
    Date expiryDate = RandomGenerator.getRandomCalendar().getTime();
    result.setExpiry(expiryDate);
    result.setFilename("toto"); //shouldn't change
    int majorVersion = 5;
    result.setMajorVersion(majorVersion);
    int minorVersion = 10;
    result.setMinorVersion(minorVersion);
    int order = 5000;
    result.setOrder(order);
    String title = "Mon document de test mis à jour";
    result.setTitle(title);
    instance.updateAttachment(result, false, false);
    result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result, is(notNullValue()));
    assertThat(result.getAlert(), is(DateUtil.getBeginOfDay(alertDate)));
    assertThat(result.getContentType(), is(MimeTypes.BZ2_ARCHIVE_MIME_TYPE));
  }

  @Test
  public void updateAttachmentForbidRoles() {
    SimpleDocument documentUpdated = instance.searchDocumentById(existingFrDoc, null);
    assertThat(documentUpdated.getForbiddenDownloadForRoles(), nullValue());

    // Adding roles that adds technically the downloadable mixin to the SimpleDocument node
    documentUpdated
        .addRolesForWhichDownloadIsForbidden(SilverpeasRole.READER, SilverpeasRole.WRITER);
    instance.updateAttachment(documentUpdated, false, false);
    SimpleDocument result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result, is(notNullValue()));
    assertThat(result, not(sameInstance(documentUpdated)));
    assertThat(result.getForbiddenDownloadForRoles(),
        contains(SilverpeasRole.WRITER, SilverpeasRole.READER));

    // Allowing writers here updates the list of forbidden roles
    documentUpdated.addRolesForWhichDownloadIsAllowed(SilverpeasRole.WRITER);
    instance.updateAttachment(documentUpdated, false, false);
    result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result.getForbiddenDownloadForRoles(), contains(SilverpeasRole.READER));

    // Allowing readers here cleans up the list of forbidden roles and technically removes the
    // downloadable mixin from the SimpleDocument node
    documentUpdated.addRolesForWhichDownloadIsAllowed(SilverpeasRole.READER);
    instance.updateAttachment(documentUpdated, false, false);
    result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result.getForbiddenDownloadForRoles(), nullValue());
  }

  @Test
  public void switchAllowingDownloadForReaders() {
    SimpleDocument documentUpdated = instance.searchDocumentById(existingFrDoc, null);
    assertThat(documentUpdated.getForbiddenDownloadForRoles(), nullValue());

    // Forbid download for readers
    instance.switchAllowingDownloadForReaders(documentUpdated.getPk(), false);

    SimpleDocument result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result, is(notNullValue()));
    assertThat(result, not(sameInstance(documentUpdated)));
    assertThat(result.getForbiddenDownloadForRoles(),
        contains(SilverpeasRole.USER, SilverpeasRole.READER));

    // Allow download for readers
    instance.switchAllowingDownloadForReaders(documentUpdated.getPk(), true);
    result = instance.searchDocumentById(existingFrDoc, null);
    assertThat(result.getForbiddenDownloadForRoles(), nullValue());
  }

  @Test
  public void listDocumentsRequiringWarning() {
    ByteArrayInputStream content =
        new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    String owner = "10";
    Calendar today = Calendar.getInstance();
    DateUtil.setAtBeginOfDay(today);
    SimpleAttachment attachment1 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument warningDoc1 =
        new SimpleDocument(emptyId, foreignId, 10, false, owner, attachment1);
    warningDoc1.setAlert(today.getTime());
    instance.createAttachment(warningDoc1, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument notWarningDoc2 =
        new SimpleDocument(emptyId, foreignId, 15, false, owner, attachment2);
    notWarningDoc2.setAlert(RandomGenerator.getCalendarAfter(today)
        .getTime());
    instance.createAttachment(notWarningDoc2, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment3 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument warningDoc3 =
        new SimpleDocument(emptyId, foreignId, 20, false, owner, attachment3);
    warningDoc3.setAlert(today.getTime());
    instance.createAttachment(warningDoc3, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment4 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument notWarningDoc4 =
        new SimpleDocument(emptyId, foreignId, 25, false, owner, attachment4);
    Calendar beforeDate = RandomGenerator.getCalendarBefore(today);
    notWarningDoc4.setAlert(beforeDate.getTime());
    instance.createAttachment(notWarningDoc4, content);
    List<SimpleDocument> docs = instance.listDocumentsRequiringWarning(today.getTime(), null);
    assertThat(docs, is(notNullValue()));
    assertThat(docs.size(), is(2));
    assertThat(docs, contains(warningDoc1, warningDoc3));
  }

  @Test
  public void listExpiringDocuments() {
    ByteArrayInputStream content =
        new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    String owner = "10";
    Calendar today = Calendar.getInstance();
    DateUtil.setAtBeginOfDay(today);
    SimpleAttachment attachment1 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument expiringDoc1 =
        new SimpleDocument(emptyId, foreignId, 10, false, owner, attachment1);
    expiringDoc1.setExpiry(today.getTime());
    instance.createAttachment(expiringDoc1, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument notExpiringDoc2 =
        new SimpleDocument(emptyId, foreignId, 15, false, owner, attachment2);
    notExpiringDoc2.setExpiry(RandomGenerator.getCalendarAfter(today)
        .getTime());
    instance.createAttachment(notExpiringDoc2, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment3 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument expiringDoc3 =
        new SimpleDocument(emptyId, foreignId, 20, false, owner, attachment3);
    expiringDoc3.setExpiry(today.getTime());
    instance.createAttachment(expiringDoc3, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment4 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument notExpiringDoc4 =
        new SimpleDocument(emptyId, foreignId, 25, false, owner, attachment4);
    Calendar beforeDate = RandomGenerator.getCalendarBefore(today);
    notExpiringDoc4.setExpiry(beforeDate.getTime());
    instance.createAttachment(notExpiringDoc4, content);
    List<SimpleDocument> docs = instance.listExpiringDocuments(today.getTime(), "fr");
    assertThat(docs, is(notNullValue()));
    assertThat(docs.size(), is(2));
    assertThat(docs, contains(expiringDoc1, expiringDoc3));
  }

  @Test
  public void listDocumentsToUnlock() {
    ByteArrayInputStream content =
        new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String foreignId = "node18";
    String owner = "10";
    Calendar today = Calendar.getInstance();
    DateUtil.setAtBeginOfDay(today);
    SimpleAttachment attachment1 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument docToLeaveLocked1 =
        new SimpleDocument(emptyId, foreignId, 10, false, owner, attachment1);
    docToLeaveLocked1.setExpiry(today.getTime());
    instance.createAttachment(docToLeaveLocked1, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument docToUnlock2 =
        new SimpleDocument(emptyId, foreignId, 15, false, owner, attachment2);
    docToUnlock2.setExpiry(RandomGenerator.getCalendarBefore(today)
        .getTime());
    instance.createAttachment(docToUnlock2, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment3 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("18")
        .build();
    SimpleDocument docToUnlock3 =
        new SimpleDocument(emptyId, foreignId, 20, false, owner, attachment3);
    docToUnlock3.setExpiry(RandomGenerator.getCalendarBefore(today)
        .getTime());
    instance.createAttachment(docToUnlock3, content);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment4 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", RandomGenerator.getRandomCalendar()
            .getTime())
        .setFormId("5")
        .build();
    SimpleDocument docToLeaveLocked4 =
        new SimpleDocument(emptyId, foreignId, 25, false, owner, attachment4);
    Calendar beforeDate = RandomGenerator.getCalendarAfter(today);
    docToLeaveLocked4.setExpiry(beforeDate.getTime());
    instance.createAttachment(docToLeaveLocked4, content);
    List<SimpleDocument> docs = instance.listDocumentsToUnlock(today.getTime(), "fr");
    assertThat(docs, is(notNullValue()));
    assertThat(docs.size(), is(2));
    assertThat(docs, contains(docToUnlock2, docToUnlock3));
  }

  @Test
  public void listDocumentLockedByUser() {
    ByteArrayInputStream content =
        new ByteArrayInputStream("This is a test".getBytes(Charsets.UTF_8));
    SimpleDocumentPK emptyId = new SimpleDocumentPK("-1", instanceId);
    String otherInstanceId = "kmelia38";
    String otherOwner = "25";
    String foreignId = "node18";
    String owner = "10";
    Calendar today = Calendar.getInstance();
    DateUtil.setAtBeginOfDay(today);
    Date createdDate = RandomGenerator.getRandomCalendar()
        .getTime();
    SimpleAttachment attachment1 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", createdDate)
        .setFormId("18")
        .build();
    SimpleDocument docToLeaveLocked1 =
        new SimpleDocument(emptyId, foreignId, 10, false, otherOwner, attachment1);
    docToLeaveLocked1.setExpiry(today.getTime());
    instance.createAttachment(docToLeaveLocked1, content);
    emptyId = new SimpleDocumentPK("-1", otherInstanceId);
    createdDate = new Date(createdDate.getTime() + 36000000);
    SimpleAttachment attachment2 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", createdDate)
        .setFormId("5")
        .build();
    SimpleDocument docToUnlock2 =
        new SimpleDocument(emptyId, foreignId, 15, false, owner, attachment2);
    docToUnlock2.setExpiry(RandomGenerator.getCalendarBefore(today)
        .getTime());
    instance.createAttachment(docToUnlock2, content);
    createdDate = new Date(createdDate.getTime() + 36000000);
    emptyId = new SimpleDocumentPK("-1", instanceId);
    SimpleAttachment attachment3 = SimpleAttachment.builder("en")
        .setFilename("test.pdf")
        .setTitle("My test document")
        .setDescription("This is a test document")
        .setSize("This is a test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.PDF_MIME_TYPE)
        .setCreationData("0", createdDate)
        .setFormId("18")
        .build();
    SimpleDocument docToUnlock3 =
        new SimpleDocument(emptyId, foreignId, 20, false, owner, attachment3);
    docToUnlock3.setExpiry(RandomGenerator.getCalendarBefore(today)
        .getTime());
    instance.createAttachment(docToUnlock3, content);
    createdDate = new Date(createdDate.getTime() + 36000000);
    emptyId = new SimpleDocumentPK("-1", otherInstanceId);
    SimpleAttachment attachment4 = SimpleAttachment.builder("fr")
        .setFilename("test.odp")
        .setTitle("Mon document de test")
        .setDescription("Ceci est un document de test")
        .setSize("Ceci est un test".getBytes(Charsets.UTF_8).length)
        .setContentType(MimeTypes.MIME_TYPE_OO_PRESENTATION)
        .setCreationData("10", createdDate)
        .setFormId("5")
        .build();
    SimpleDocument docToLeaveLocked4 =
        new SimpleDocument(emptyId, foreignId, 25, false, otherOwner, attachment4);
    Calendar beforeDate = RandomGenerator.getCalendarAfter(today);
    docToLeaveLocked4.setExpiry(beforeDate.getTime());
    instance.createAttachment(docToLeaveLocked4, content);
    List<SimpleDocument> docs = instance.listDocumentsLockedByUser(owner, "fr");
    assertThat(docs, is(notNullValue()));
    assertThat(docs.size(), is(2));
    assertThat(docs, contains(docToUnlock3, docToUnlock2));
  }

  private void checkFrenchSimpleDocument(SimpleDocument doc) {
    assertThat(doc, is(notNullValue()));
    assertThat(doc.getContentType(), is(MimeTypes.MIME_TYPE_OO_PRESENTATION));
    assertThat(doc.getSize(), is((long) ("Ceci est un test".getBytes(Charsets.UTF_8).length)));
    assertThat(doc.getDescription(), is("Ceci est un document de test"));
    File file = new File(doc.getAttachmentPath());
    assertThat(file, is(notNullValue()));
    assertThat(file.exists(), is(true));
    assertThat(file.isFile(), is(true));
    assertThat(file.length(), is((long) ("Ceci est un test".getBytes(Charsets.UTF_8).length)));
  }

  private void checkFrenchFileSimpleDocument(SimpleDocument doc) {
    assertThat(doc, is(notNullValue()));
    assertThat(doc.getContentType(), is(MimeTypes.MIME_TYPE_OO_PRESENTATION));
    assertThat(doc.getSize(), is(12929L));
    assertThat(doc.getDescription(), is("Ceci est un document de test"));
    File file = new File(doc.getAttachmentPath());
    assertThat(file, is(notNullValue()));
    assertThat(file.exists(), is(true));
    assertThat(file.isFile(), is(true));
    assertThat(file.length(), is(12929L));
  }

  private void checkEnglishSimpleDocument(SimpleDocument doc) {
    assertThat(doc, is(notNullValue()));
    assertThat(doc.getContentType(), is(MimeTypes.WORD_2007_MIME_TYPE));
    assertThat(doc.getSize(), is((long) ("This is a test".getBytes(Charsets.UTF_8).length)));
    assertThat(doc.getDescription(), is("This is a test document"));
    assertThat(doc.getCreatedBy(), is("0"));
    File file = new File(doc.getAttachmentPath());
    assertThat(file, is(notNullValue()));
    assertThat(file.exists(), is(true));
    assertThat(file.isFile(), is(true));
    assertThat(file.length(), is((long) ("This is a test".getBytes(Charsets.UTF_8).length)));
  }

  protected File getDocumentNamed(final String name) {
    return FileUtils.getFile(getMavenTargetDirectory().getResourceTestDirFile(), name);
  }
}
