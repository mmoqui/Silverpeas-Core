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
package org.silverpeas.core.pdc.pdc.service;

import org.silverpeas.core.ResourceReference;
import org.silverpeas.core.admin.service.OrganizationControllerProvider;
import org.silverpeas.core.annotation.Service;
import org.silverpeas.core.contribution.contentcontainer.content.*;
import org.silverpeas.core.contribution.publication.model.PublicationPK;
import org.silverpeas.core.i18n.I18NHelper;
import org.silverpeas.core.index.indexing.model.IndexEngineProxy;
import org.silverpeas.core.pdc.classification.ClassifyEngine;
import org.silverpeas.core.pdc.classification.ObjectValuePair;
import org.silverpeas.core.pdc.classification.PertinentAxis;
import org.silverpeas.core.pdc.classification.Position;
import org.silverpeas.core.pdc.pdc.model.*;
import org.silverpeas.core.pdc.subscription.service.PdcSubscriptionManager;
import org.silverpeas.core.pdc.tree.model.TreeManagerException;
import org.silverpeas.core.pdc.tree.model.TreeNode;
import org.silverpeas.core.pdc.tree.model.TreeNodePK;
import org.silverpeas.core.pdc.tree.service.TreeService;
import org.silverpeas.core.persistence.jdbc.DBUtil;
import org.silverpeas.core.persistence.jdbc.bean.BeanCriteria;
import org.silverpeas.core.persistence.jdbc.bean.PersistenceException;
import org.silverpeas.core.persistence.jdbc.bean.SilverpeasBeanDAO;
import org.silverpeas.core.persistence.jdbc.bean.SilverpeasBeanDAOFactory;
import org.silverpeas.core.security.authorization.AccessControlContext;
import org.silverpeas.core.security.authorization.PublicationAccessControl;
import org.silverpeas.core.util.DateUtil;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.kernel.logging.SilverLogger;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.silverpeas.core.admin.component.model.SilverpeasComponentInstance.getComponentName;
import static org.silverpeas.core.persistence.jdbc.bean.BeanCriteria.OPERATOR.GREATER_OR_EQUAL;
import static org.silverpeas.core.security.authorization.AccessControlOperation.SEARCH;
import static org.silverpeas.core.util.CollectionUtil.isEmpty;

@Service
@SuppressWarnings("deprecation")
public class GlobalPdcManager implements PdcManager {

  private static final String UNKNOWN = "unknown";
  private static final String KMELIA_COMPONENT_NAME = "kmelia";
  private static final String AXIS_TYPE = "AxisType";
  private static final String AXIS_ORDER = "AxisOrder";

  /**
   * SilverpeasBeanDAO is the main link with the SilverPeas persistence. We indicate the Object
   * SilverPeas which map the database.
   */
  private SilverpeasBeanDAO<AxisHeaderPersistence> dao = null;

  @Inject
  private AxisHeaderI18NDAO axisHeaderI18NDAO;
  /**
   * PdcUtilizationBm, the pdc utilization interface to manage which axis are used by which
   * instance
   */
  @Inject
  private PdcUtilizationService pdcUtilizationService;
  /**
   * PdcClassifyBm, the pdc classify interface to manage how are classified object in the pdc
   */
  @Inject
  private PdcClassifyManager pdcClassifyManager;
  @Inject
  private ContentManagementEngine contentMgtEngine;
  @Inject
  private PdcClassificationService pdcClassificationService;
  @Inject
  private PdcSubscriptionManager pdcSubscriptionManager;
  @Inject
  private TreeService treeService;

  private static final Map<String, AxisHeader> axisHeaders =
      Collections.synchronizedMap(new HashMap<>());

  /**
   * Constructor declaration
   */
  protected GlobalPdcManager() {
    try {
      dao = SilverpeasBeanDAOFactory.getDAO(AxisHeaderPersistence.class);
    } catch (PersistenceException exceDAO) {
      SilverLogger.getLogger(this).error("Cannot get DAO for AxisHeader", exceDAO);
    }
  }

  @Override
  public List<GlobalSilverContent> findGlobalSilverContents(
      SearchContext containerPosition, List<String> componentIds,
      boolean recursiveSearch, boolean visibilitySensitive) {
    List<Integer> silverContentIds;
    try {
      // get the silverContentids classified in the context
      silverContentIds = new ArrayList<>(findSilverContentIdByPosition(containerPosition,
          componentIds, recursiveSearch, visibilitySensitive));
    } catch (PdcException c) {
      throw new PdcRuntimeException(c);
    }

    return getSilverContentsByIds(silverContentIds, containerPosition.getUserId());
  }

  /**
   * Returns a list of axes sorted in according to the axe type.
   * @param type - the whished type of the axe.
   * @return a sorted list.
   */
  @Override
  public List<AxisHeader> getAxisByType(String type) throws PdcException {
    try {
      BeanCriteria criteria = BeanCriteria.addCriterion(AXIS_TYPE, type);
      criteria.setAscOrderBy(AXIS_ORDER);
      Collection<AxisHeaderPersistence> axis = dao.findBy(criteria);
      return persistence2AxisHeaders(axis);
    } catch (PersistenceException exSelect) {
      throw new PdcException(exSelect);
    }
  }

  private List<AxisHeader> persistence2AxisHeaders(
      Collection<AxisHeaderPersistence> silverpeasBeans) {
    List<AxisHeader> resultingAxisHeaders = new ArrayList<>();
    if (silverpeasBeans != null) {
      for (AxisHeaderPersistence silverpeasBean : silverpeasBeans) {
        AxisHeader axisHeader = new AxisHeader(silverpeasBean);
        // ajout des traductions
        setTranslations(axisHeader);
        resultingAxisHeaders.add(axisHeader);
      }
    }
    return resultingAxisHeaders;
  }

  private void setTranslations(AxisHeader axisHeader) {
    // ajout de la traduction par defaut
    String axisId = axisHeader.getPK().getId();
    AxisHeaderI18N translation = new AxisHeaderI18N(axisId, axisHeader.getLanguage(), axisHeader.
        getName(), axisHeader.getDescription());
    axisHeader.addTranslation(translation);
    // récupération des autres traductions
    Connection con = null;
    try {
      con = openConnection();
      List<AxisHeaderI18N> translations =
          axisHeaderI18NDAO.getTranslations(con, axisId);
      for (AxisHeaderI18N tr : translations) {
        axisHeader.addTranslation(tr);
      }
    } catch (Exception e) {
      SilverLogger.getLogger(this).error("Error to set translations for axis {0}",
          new String[]{axisHeader.getName()}, e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * Returns a list of axes sorted.
   * @return a list sorted or null otherwise
   */
  @Override
  public List<AxisHeader> getAxis() throws PdcException {
    try {
      BeanCriteria criteria = BeanCriteria.emptyCriteria();
      criteria.setAscOrderBy(AXIS_TYPE, AXIS_ORDER);
      Collection<AxisHeaderPersistence> axis = dao.findBy(criteria);
      return persistence2AxisHeaders(axis);
    } catch (PersistenceException exSelect) {
      throw new PdcException(exSelect);
    }

  }

  /**
   * Return the number of axe.
   * @return the number of axe
   */
  @Override
  public int getNbAxis() throws PdcException {
    return getAxis().size();
  }

  /**
   * Return the max number of axis.
   * @return the max number of axis
   */
  @Override
  public int getNbMaxAxis() {
    return ClassifyEngine.getMaxAxis();
  }

  /**
   * Create an axe into the data base.
   * @param axisHeader - the object which contains all data about an axe
   * @return 1 if the maximun of axe is atteignable, 2 if the axe already exist, 0 otherwise
   */
  @Override
  public int createAxis(AxisHeader axisHeader) throws PdcException {

    int status = 0;
    List<AxisHeader> axis = getAxis();

    // search if the maximun number of axes is atteignable
    if (axis.size() > getNbMaxAxis()) {
      status = 1;
    } else if (isAxisNameExist(axis, axisHeader)) {
      status = 2;
    } else {
      Connection con = null;
      try {
        con = openTransaction();
        int order = axisHeader.getAxisOrder();
        String type = axisHeader.getAxisType();
        // recupere les axes de meme type ordonnés qui ont un numéro d'ordre
        // >= à celui de l'axe à inserer
        BeanCriteria criteria = BeanCriteria.addCriterion(AXIS_TYPE, type)
            .and(AXIS_ORDER, GREATER_OR_EQUAL, order);
        criteria.setAscOrderBy(AXIS_ORDER);
        // ATTENTION il faut traiter l'ordre des autres axes
        Collection<AxisHeaderPersistence> axisToUpdate =
            dao.findBy(criteria);

        for (AxisHeaderPersistence axisToMove : axisToUpdate) {
          // On modifie l'ordre de l'axe en ajoutant 1 par rapport au nouvel axe
          order++;
          axisToMove.setAxisOrder(order);
          dao.update(con, axisToMove);
        }

        // build of the Value
        Value value = new Value(UNKNOWN, Integer.toString(axisHeader.getRootId()));
        value.setName(axisHeader.getName());
        value.setDescription(axisHeader.getDescription());
        value.setCreationDate(axisHeader.getCreationDate());
        value.setCreatorId(axisHeader.getCreatorId());
        value.setPath(UNKNOWN);
        value.setOrderNumber(-1);
        value.setLevelNumber(-1);
        value.setFatherId(UNKNOWN);
        value.setLanguage(axisHeader.getLanguage());
        value.setRemoveTranslation(axisHeader.isRemoveTranslation());
        value.setTranslationId(axisHeader.getTranslationId());

        String treeId = treeService.createRoot(con, value);

        axisHeader.setRootId(Integer.parseInt(treeId));

        AxisHeaderPersistence ahp = new AxisHeaderPersistence(axisHeader);
        AxisPK axisPK = (AxisPK) dao.add(con, ahp);

        // Register new axis to classifyEngine
        pdcClassifyManager.registerAxis(con, Integer.parseInt(axisPK.getId()));

        commitTransaction(con);
      } catch (Exception exCreate) {
        rollbackTransaction(con);
        throw new PdcException(exCreate);
      } finally {
        DBUtil.close(con);
      }
    }

    return status;
  }

  /**
   * Update an axe into the data base.
   * @param axisHeader - the object which contains all data about an axe
   * @return 2 if the axe already exist, 0 otherwise
   */
  @Override
  public int updateAxis(AxisHeader axisHeader) throws PdcException {

    int status = 0;
    List<AxisHeader> axis = getAxis();

    if (isAxisNameExist(axis, axisHeader)) {
      status = 2;
    } else {
      Connection con = null;
      try {
        con = openTransaction();
        // si order = -1 alors l'ordre ne doit pas être modifié
        int order = axisHeader.getAxisOrder();

        if (order != -1) {
          order = processOrdering(axisHeader, order, con);
        }
        // update root value linked to this axis

        AxisHeader oldAxisHeader = getAxisHeader(con, axisHeader.getPK().getId());


        // regarder si le nom et la description ont changé en fonction de la langue
        updateNameAndDescription(axisHeader, oldAxisHeader, con);

        // update axis
        axisHeader.setRootId(oldAxisHeader.getRootId());
        axisHeader.setCreationDate(oldAxisHeader.getCreationDate());
        axisHeader.setCreatorId(oldAxisHeader.getCreatorId());
        if (order == -1) {
          // order has not changed
          axisHeader.setAxisOrder(oldAxisHeader.getAxisOrder());
        }

        // gestion des traductions
        manageTranslations(axisHeader, oldAxisHeader, con);

        // remove axisheader from cache
        axisHeaders.remove(axisHeader.getPK().getId());

        commitTransaction(con);
      } catch (Exception exUpdate) {
        rollbackTransaction(con);
        throw new PdcException(exUpdate);
      } finally {
        DBUtil.close(con);
      }
    }

    return status;
  }

  private void manageTranslations(AxisHeader axisHeader, AxisHeader oldAxisHeader,
      Connection con) throws SQLException, PersistenceException {
    if (axisHeader.isRemoveTranslation()) {
      deleteTranslations(axisHeader, oldAxisHeader, con);
    } else {
      updateTranslations(axisHeader, oldAxisHeader, con);

      AxisHeaderPersistence axisHP = new AxisHeaderPersistence(axisHeader);
      dao.update(con, axisHP);
    }
  }

  private void updateTranslations(AxisHeader axisHeader, AxisHeader oldAxisHeader,
      Connection con) throws SQLException {
    if (axisHeader.getLanguage() != null) {
      if (oldAxisHeader.getLanguage() == null) {
        // translation for the first time
        oldAxisHeader.setLanguage(I18NHelper.DEFAULT_LANGUAGE);
      }
      if (!axisHeader.getLanguage().equalsIgnoreCase(oldAxisHeader.getLanguage())) {
        AxisHeaderI18N newAxis =
            new AxisHeaderI18N(axisHeader.getPK().getId(),
                axisHeader.getLanguage(), axisHeader.getName(), axisHeader.getDescription());
        String translationId = axisHeader.getTranslationId();
        if (translationId != null && !translationId.equals("-1")) {
          // update translation
          newAxis.setId(axisHeader.getTranslationId());

          axisHeaderI18NDAO.updateTranslation(con, newAxis);
        } else {
          axisHeaderI18NDAO.createTranslation(con, newAxis);
        }

        axisHeader.setLanguage(oldAxisHeader.getLanguage());
        axisHeader.setName(oldAxisHeader.getName());
        axisHeader.setDescription(oldAxisHeader.getDescription());
      }
    }
  }

  private void deleteTranslations(AxisHeader axisHeader, AxisHeader oldAxisHeader,
      Connection con) throws SQLException, PersistenceException {
    if (oldAxisHeader.getLanguage() == null) {
      // translation for the first time
      oldAxisHeader.setLanguage(I18NHelper.DEFAULT_LANGUAGE);
    }
    if (oldAxisHeader.getLanguage().equalsIgnoreCase(axisHeader.getLanguage())) {
      List<AxisHeaderI18N> translations =
          axisHeaderI18NDAO.getTranslations(con, axisHeader.getPK().getId());

      if (translations != null && !translations.isEmpty()) {
        AxisHeaderI18N translation = translations.get(0);

        axisHeader.setLanguage(translation.getLanguage());
        axisHeader.setName(translation.getName());
        axisHeader.setDescription(translation.getDescription());

        AxisHeaderPersistence axisHP = new AxisHeaderPersistence(axisHeader);
        dao.update(con, axisHP);

        axisHeaderI18NDAO.deleteTranslation(con, translation.getId());
      }
    } else {
      axisHeaderI18NDAO
          .deleteTranslation(con, axisHeader.getTranslationId());
    }
  }

  private void updateNameAndDescription(AxisHeader axisHeader, AxisHeader oldAxisHeader,
      Connection con) throws TreeManagerException {
    boolean axisNameHasChanged = false;
    boolean axisDescHasChanged = false;

    if (oldAxisHeader.getName() != null &&
        !oldAxisHeader.getName().equalsIgnoreCase(axisHeader.getName())) {
      axisNameHasChanged = true;
    }
    if ((oldAxisHeader.getDescription() != null &&
        !oldAxisHeader.getDescription().equalsIgnoreCase(axisHeader.getDescription())) ||
        (oldAxisHeader.getDescription() == null && axisHeader.getDescription() != null)) {
      axisDescHasChanged = true;
    }

    if (axisNameHasChanged || axisDescHasChanged) {
      // The name of the axis has changed, We must change the name of the root to
      String treeId = Integer.toString(oldAxisHeader.getRootId());
      TreeNode root = treeService.getRoot(con, treeId);
      TreeNode node = createTreeNode(axisHeader, root);
      treeService.updateRoot(con, node);
    }
  }

  private int processOrdering(AxisHeader axisHeader, int order, Connection con) throws PersistenceException {
    String type = axisHeader.getAxisType();
    String axisId = axisHeader.getPK().getId();
    // recupere les axes de meme type ordonnés qui ont un numéro d'ordre >= à celui de
    // l'axe à inserer
    BeanCriteria criteria = BeanCriteria.addCriterion(AXIS_TYPE, type)
        .and(AXIS_ORDER, GREATER_OR_EQUAL, order);
    criteria.setAscOrderBy(AXIS_ORDER);

    // ATTENTION il faut traiter l'ordre des autres axes
    Collection<AxisHeaderPersistence> axisToUpdate = dao.findBy(con, criteria);

    boolean axisHasMoved = true;
    Iterator<AxisHeaderPersistence> it = axisToUpdate.iterator();
    AxisHeaderPersistence firstAxis;

    if (it.hasNext()) {
      // Test si l'axe n'a pas changé de place
      firstAxis = it.next();
      if (firstAxis.getPK().getId().equals(axisId)) {
        axisHasMoved = false;
      }
    }

    if (axisHasMoved) {
      for (AxisHeaderPersistence axisToMove : axisToUpdate) {
        // On modifie l'ordre de l'axe en ajoutant 1 par rapport au nouvel axe
        order++;
        axisToMove.setAxisOrder(order);
        dao.update(con, axisToMove);
        // remove axisheader from cache
        axisHeaders.remove(axisHeader.getPK().getId());
      }
    }
    return order;
  }

  private static TreeNode createTreeNode(AxisHeader axisHeader, TreeNode root) {
    TreeNode node = new TreeNode(root.getPK().getId(), root.getTreeId());
    node.setName(axisHeader.getName());
    node.setDescription(axisHeader.getDescription());
    node.setCreationDate(root.getCreationDate());
    node.setCreatorId(root.getCreatorId());
    node.setPath(root.getPath());
    node.setLevelNumber(root.getLevelNumber());
    node.setOrderNumber(root.getOrderNumber());
    node.setFatherId(root.getFatherId());
    node.setLanguage(axisHeader.getLanguage());
    node.setRemoveTranslation(axisHeader.isRemoveTranslation());
    node.setTranslationId(axisHeader.getTranslationId());
    node.setTranslationsFrom(axisHeader.getTranslations());
    return node;
  }

  /**
   * delete the axe from the data base and all its subtrees.
   * @param axisId - the id of the selected axe
   */
  @Override
  public void deleteAxis(Connection con, String axisId) throws PdcException {
    try {
      // get the header of the axe to obtain the rootId.
      AxisHeader axisHeader = getAxisHeader(con, axisId);

      PdcRightsDAO.deleteAxisRights(con, axisId);

      pdcClassificationService.axisDeleted(axisId);

      // delete data in the treeService table
      treeService.deleteTree(con, Integer.toString(axisHeader.getRootId()));
      // delete data in the pdc utilization table
      pdcUtilizationService.deleteUsedAxisByAxisId(con, axisId);
      dao.remove(con, new AxisPK(axisId));

      // Unregister axis to classifyEngine
      pdcClassifyManager.unregisterAxis(con, Integer.parseInt(axisId));
      pdcSubscriptionManager.checkAxisOnDelete(Integer.parseInt(axisId), axisHeader.getName());

      // remove axisheader from cache
      axisHeaders.remove(axisId);

      // suppression des traductions
      axisHeaderI18NDAO.deleteTranslations(con, axisId);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  /**
   * Returns a detail axe (header,values).
   * @param axisId - the id of the selected axe.
   * @return the Axis Object
   */
  @Override
  public Axis getAxisDetail(String axisId) throws PdcException {
    Axis axis = null;
    // get the header of the axe to obtain the rootId.
    AxisHeader axisHeader = getAxisHeader(axisId);
    if (axisHeader != null) {
      int treeId = axisHeader.getRootId();
      axis = new Axis(axisHeader, getAxisValues(treeId));
    }
    return axis;
  }

  @Override
  public String getTreeId(String axisId) throws PdcException {
    // get the header of the axis to obtain the rootId.
    AxisHeaderPersistence axisHeader = getAxisHeaderPersistence(axisId);
    int treeId = -1;
    if (axisHeader != null) {
      treeId = axisHeader.getRootId();
    }
    return Integer.toString(treeId);
  }

  /**
   * Returns a value from an axe.
   * @param valueId - the id of the selected value
   * @return the Value object
   */
  @Override
  public Value getValue(String axisId, String valueId)
      throws PdcException {
    try (Connection con = openConnection()) {
      TreeNode node = treeService.getNode(con, new TreeNodePK(valueId), getTreeId(axisId));
      return createValue(node);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  /**
   * Returns a value from an axe.
   * @param valueId - the id of the selected value
   * @return the Value object
   */
  @Override
  public Value getAxisValue(String valueId, String treeId)
      throws PdcException {
    Connection con = null;
    try {
      con = openConnection();
      return createValue(treeService.getNode(con, new TreeNodePK(valueId), treeId));
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * Return a list of axis values having the value name in parameter
   * @param valueName the name of the value.
   * @return a list of axis values.
   * @throws PdcException if an error occurs while getting the axis values.
   */
  @Override
  public List<Value> getAxisValuesByName(String valueName) throws PdcException {
    try (Connection con = openConnection()) {
      List<TreeNode> listTreeNodes = treeService.getNodesByName(con, valueName);
      return createValuesList(listTreeNodes);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  /**
   * Return a list of String corresponding to the valueId of the value in parameter
   * @param axisId axis identifier
   * @param valueId value identifier
   * @return List of String
   * @throws PdcException if an error occurs while getting the values
   */
  @Override
  public List<String> getDaughterValues(String axisId, String valueId) throws PdcException {
    List<String> listValuesString = new ArrayList<>();
    Connection con = openConnection();

    try {
      List<Value> listValues = getDaughters(con, valueId, axisId);
      for (Value value : listValues) {
        listValuesString.add(value.getPK().getId());
      }
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }

    return listValuesString;
  }

  /**
   * Return the Value corresponding to the axis done
   * @param axisId the axis identifier
   * @return org.silverpeas.core.pdc.pdc.model.Value
   * @throws PdcException while getting the root value of the specified axis.
   */
  @Override
  public Value getRoot(String axisId) throws PdcException {
    Connection con = openConnection();
    try {
      // get the header of the axis to obtain the rootId.
      AxisHeader axisHeader = getAxisHeader(axisId, false);
      int treeId = axisHeader.getRootId();
      TreeNode treeNode = treeService.getRoot(con, Integer.toString(treeId));
      return createValue(treeNode);
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * @param treeId The id of the selected axis.
   * @return The list of values of the axis.
   */
  @Override
  public List<Value> getAxisValues(int treeId) throws PdcException {
    Connection con = openConnection();

    try {
      return createValuesList(treeService.getTree(con, Integer.toString(treeId)));
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * insert a value which is defined like a mother value
   * @param valueToInsert - a Value object
   * @param refValue - the id of the Value to insert
   * @return 1 if the name already exist 0 otherwise
   */
  @Override
  public int insertMotherValue(Value valueToInsert,
      String refValue, String axisId) throws PdcException {
    int status;
    // get the header of the axis to obtain the treeId.
    AxisHeader axisHeader = getAxisHeader(axisId, false);
    String treeId = Integer.toString(axisHeader.getRootId());

    // get the mother value of the value which have the refValue
    // to find sisters of the valueToInsert
    TreeNode refNode = getAxisValue(refValue, treeId);

    Connection con = null;

    try {
      con = openTransaction();
      // Avant l'insertion de la mere, on recupere les vieux chemins
      ArrayList<String> oldPath = getPathes(con, refValue, treeId);

      if (refNode.getLevelNumber() != 0) {
        status = insertMotherValueToValue(con, valueToInsert, refValue, treeId);
      } else {
        insertMotherValueToRootValue(con, valueToInsert, refValue, treeId);
        status = 0;
      }

      // Warning, we must update the path of the Value(Classify)
      if ((status == 0) && !oldPath.isEmpty()) {
        // the mother value is created

        // Après l'insertion de la mere, on recupere les nouveaux chemins
        ArrayList<String> newPath = getPathes(con, refValue, treeId);
        // call the ClassifyBm to create oldValue and newValue
        // and to replace the oldValue by the newValue
        pdcClassifyManager.createValuesAndReplace(con, axisId, oldPath, newPath);
      }

      commitTransaction(con);
    } catch (Exception e) {
      rollbackTransaction(con);
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }

    return status;
  }

  /**
   * Move a value under a new father
   * @param axis : l'axe concerné
   * @param valueToMove - a Value object
   * @param newFatherId - the id of the new father
   * @return 1 if the name already exist 0 otherwise
   */
  @Override
  public int moveValueToNewFatherId(Axis axis, Value valueToMove, String newFatherId,
      int orderNumber) throws PdcException {
    int status = 0;
    String treeId = Integer.toString(axis.getAxisHeader().getRootId());
    String valueToMoveId = valueToMove.getPK().getId();
    Connection con = null;

    try {
      con = openTransaction();
      // Avant le déplassement de la valeur, on recupere les vieux chemins afin
      // de reclasser les associations après
      ArrayList<String> oldPath = getPathes(con, valueToMoveId, treeId);
      // il ne faut pas que la valeur que l'on insère ai une soeur du même nom
      List<Value> daughters = getDaughters(con, newFatherId, treeId);
      if (isValueNameExist(daughters, valueToMove)) {
        status = 1;
      } else {
        moveSubTree(con, treeId, valueToMoveId, newFatherId, orderNumber);
      }

      // Warning, we must update the path of the Value(Classify)
      if ((status == 0) && !oldPath.isEmpty()) {
        // the mother value is created
        // Après l'insertion de la mere, on recupere les nouveaux chemins
        ArrayList<String> newPath = getPathes(con, valueToMoveId, treeId);
        // call the ClassifyBm to create oldValue and newValue
        // and to replace the oldValue by the newValue
        pdcClassifyManager
            .createValuesAndReplace(con, Integer.toString(axis.getAxisHeader().getRootId()),
                oldPath, newPath);
      }
      commitTransaction(con);
    } catch (Exception e) {
      rollbackTransaction(con);
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
    return status;
  }

  private void moveSubTree(final Connection con, final String treeId, final String valueToMoveId,
      final String newFatherId, final int orderNumber) throws PdcException {
    try {
      // l'idée : passer en paramètres : des TreeNodePK car le métier est
      // basé sur les Tree
      treeService.moveSubTreeToNewFather(con, new TreeNodePK(valueToMoveId),
          new TreeNodePK(newFatherId), treeId, orderNumber);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  @Override
  public List<List<String>> getInheritedManagers(Value value) throws PdcException {
    String axisId = value.getAxisId();
    String path = value.getPath();
    String[] splitPath = path.split("/");
    List<List<String>> usersAndGroups = new ArrayList<>();
    final Set<String> usersInherited = new HashSet<>();
    final Set<String> groupsInherited = new HashSet<>();
    for (int i = 1; i < splitPath.length; i++) {
      List<List<String>> managers = getManagers(axisId, splitPath[i]);
      List<String> usersId = managers.get(0);
      List<String> groupsId = managers.get(1);
      usersInherited.addAll(usersId);
      groupsInherited.addAll(groupsId);
    }
    usersAndGroups.add(new ArrayList<>(usersInherited));
    usersAndGroups.add(new ArrayList<>(groupsInherited));
    return usersAndGroups;
  }

  @Override
  public List<List<String>> getManagers(String axisId, String valueId) throws PdcException {
    List<String> usersId;
    List<String> groupsId;
    Connection con = openConnection();
    try {
      usersId = PdcRightsDAO.getUserIds(con, axisId, valueId);
      groupsId = PdcRightsDAO.getGroupIds(con, axisId, valueId);
    } catch (SQLException e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
    List<List<String>> usersAndGroups = new ArrayList<>();
    usersAndGroups.add(usersId);
    usersAndGroups.add(groupsId);
    return usersAndGroups;
  }

  @Override
  public boolean isUserManager(String userId) throws PdcException {
    if (PdcSettings.isDelegationNotEnabled()) {
      return false;
    }

    Connection con = openConnection();

    try {
      // First, check if user is directly manager of a part of PDC
      boolean isManager = PdcRightsDAO.isUserManager(con, userId);

      if (!isManager) {
        // If not, check if at least one of his groups it is
        String[] groupIds =
            OrganizationControllerProvider.getOrganisationController().getAllGroupIdsOfUser(userId);

        isManager = isGroupManager(groupIds);
      }

      return isManager;
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  private boolean isGroupManager(String[] groupIds) throws PdcException {
    Connection con = openConnection();
    try {
      return PdcRightsDAO.isGroupManager(con, groupIds);
    } catch (SQLException e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  @Override
  public void setManagers(List<String> userIds, List<String> groupIds, String axisId,
      String valueId) throws PdcException {
    Connection con = null;

    try {
      con = openTransaction();
      // supprime tous les droits sur la valeur
      PdcRightsDAO.deleteRights(con, axisId, valueId);

      for (String userId : userIds) {
        PdcRightsDAO.insertUserId(con, axisId, valueId, userId);
      }
      for (String groupId : groupIds) {
        PdcRightsDAO.insertGroupId(con, axisId, valueId, groupId);
      }
      commitTransaction(con);
    } catch (SQLException e) {
      rollbackTransaction(con);
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  @Override
  public void razManagers(String axisId, String valueId) throws PdcException {
    Connection con = openConnection();
    try {
      PdcRightsDAO.deleteRights(con, axisId, valueId);
    } catch (SQLException e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  @Override
  public void deleteManager(String userId) throws PdcException {
    Connection con = openConnection();
    try {
      PdcRightsDAO.deleteManager(con, userId);
    } catch (SQLException e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  @Override
  public void deleteGroupManager(String groupId) throws PdcException {
    Connection con = openConnection();
    try {
      PdcRightsDAO.deleteGroupManager(con, groupId);
    } catch (SQLException e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * Return treeService where the root value is the refValue.
   * @param con - the connection to the database
   * @param refValue - the id of the reference Value Object
   * @return a list of each pathes found
   */
  private ArrayList<String> getPathes(Connection con, String refValue, String treeId) {
    ArrayList<String> pathList = new ArrayList<>();
    TreeNodePK refNodePK = new TreeNodePK(refValue);
    try {
      // get a list of treeService node for one treeService node, get its path
      List<TreeNode> treeList = treeService.getSubTree(con, refNodePK, treeId);
      for (TreeNode nodeTree : treeList) {
        pathList.add(nodeTree.getPath() + nodeTree.getPK().getId() + "/");
      }
    } catch (Exception e) {
      SilverLogger.getLogger(this).warn(e);
    }
    return pathList;
  }

  private void insertMotherValueToRootValue(Connection con, Value valueToInsert, String refValue,
      String treeId) throws PdcException {
    try {
      // Insertion de la nouvelle racine
      treeService.insertFatherToNode(con, valueToInsert, new TreeNodePK(refValue), treeId);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  private int insertMotherValueToValue(Connection con,
      Value valueToInsert, String refValue, String treeId)
      throws PdcException {
    int status = 0;
    // get the mother value of the value which have the refValue
    // to find sisters of the valueToInsert
    TreeNode refNode = getAxisValue(refValue, treeId);
    List<Value> daughters = getDaughters(con, refNode.getFatherId(), treeId);

    if (isValueNameExist(daughters, valueToInsert)) {
      status = 1;
    } else {
      try {
        treeService.insertFatherToNode(con, valueToInsert, new TreeNodePK(refValue), treeId);
      } catch (Exception e) {
        throw new PdcException(e);
      }
    }
    return status;
  }

  /**
   * insert a value which is defined like a daughter value
   * @param valueToInsert - a Value object
   * @param refValue - the id of the Value to insert
   * @return 1 if the name already exist 0 otherwise
   */
  @Override
  public int createDaughterValue(Value valueToInsert,
      String refValue, String treeId) throws PdcException {
    // get the Connection object
    Connection con = openConnection();

    int status = 0;
    List<Value> daughters = getDaughters(con, refValue, treeId);

    if (isValueNameExist(daughters, valueToInsert)) {
      status = 1;
      DBUtil.close(con);
    } else {
      try {
        treeService.createSonToNode(con, valueToInsert, new TreeNodePK(refValue), treeId);
      } catch (Exception e) {
        throw new PdcException(e);
      } finally {
        DBUtil.close(con);
      }

    }

    return status;
  }

  /**
   * insert a value which is defined like a daughter value
   * @param valueToInsert - a Value object
   * @param refValue - the id of the Value to insert
   * @return -1 if the name already exists id otherwise
   */
  @Override
  public String createDaughterValueWithId(Value valueToInsert,
      String refValue, String treeId) throws PdcException {
    // get the Connection object
    Connection con = openConnection();

    String daughterId;
    List<Value> daughters = getDaughters(con, refValue, treeId);

    if (isValueNameExist(daughters, valueToInsert)) {
      daughterId = "-1";
      DBUtil.close(con);
    } else {
      try {
        daughterId =
            treeService.createSonToNode(con, valueToInsert, new TreeNodePK(refValue), treeId);
      } catch (Exception e) {
        throw new PdcException(e);
      } finally {
        DBUtil.close(con);
      }
    }
    return daughterId;
  }

  /**
   * Update the selected value
   * @param value - a Value object
   * @return 1 if the name already exist 0 otherwise
   */
  @Override
  public int updateValue(Value value, String treeId)
      throws PdcException {
    // get the Connection object
    Connection con = openConnection();

    int status = 0;

    try {
      Value oldValue =
          getAxisValue(value.getPK().getId(), treeId);
      List<Value> daughters = getDaughters(con, oldValue.getMotherId(), treeId);

      if (isValueNameExist(daughters, value)) {
        status = 1;
      } else {
        TreeNode node = buildTreeNode(treeId, value, oldValue);
        treeService.updateNode(con, node);
      }
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }

    return status;
  }

  private static TreeNode buildTreeNode(String treeId, Value value, Value oldValue) {
    TreeNode node = new TreeNode(value.getPK().getId(), treeId);
    node.setName(value.getName());
    node.setDescription(value.getDescription());
    node.setCreationDate(oldValue.getCreationDate());
    node.setCreatorId(oldValue.getCreatorId());
    node.setPath(oldValue.getPath());
    node.setLevelNumber(oldValue.getLevelNumber());
    node.setOrderNumber(value.getOrderNumber());
    node.setFatherId(oldValue.getFatherId());
    node.setLanguage(value.getLanguage());
    node.setRemoveTranslation(value.isRemoveTranslation());
    node.setTranslationId(value.getTranslationId());
    return node;
  }

  /**
   * Delete a value and it's sub treeService
   * @param valueId - the id of the select value
   */
  @Override
  public void deleteValueAndSubtree(Connection con, String valueId, String axisId, String treeId)
      throws PdcException {
    try {
      // first update any predefined classifications
      List<PdcAxisValue> valuesToDelete = new ArrayList<>();
      PdcAxisValue aValueToDelete = PdcAxisValue.aPdcAxisValue(valueId, axisId);
      valuesToDelete.add(aValueToDelete);
      valuesToDelete.addAll(findRecursivelyAllChildrenOf(aValueToDelete));
      pdcClassificationService.axisValuesDeleted(valuesToDelete);

      List<Value> pathInfo = getFullPath(valueId, treeId);

      // Mise à jour de la partie utilisation
      updateBaseValuesInInstances(con, valueId, axisId, treeId);

      // Avant l'effacement de la valeur, on recupere les vieux chemins
      ArrayList<String> oldPath = getPathes(con, valueId, treeId);


      TreeNodePK treeNodePK = new TreeNodePK(valueId);

      // On recupere le chemin de la mère
      String motherId = treeService.getNode(con, treeNodePK, treeId).getFatherId();
      TreeNodePK motherPK = new TreeNodePK(motherId);
      TreeNode mother = treeService.getNode(con, motherPK, treeId);
      String motherPath = mother.getPath() + motherId + '/';


      AxisHeader axisHeader = getAxisHeader(con, axisId);
      String axisName = axisHeader.getName();

      List<TreeNode> subtree = treeService.getSubTree(con, treeNodePK, treeId);
      treeService.deleteSubTree(con, treeNodePK, treeId);

      // on efface les droits sur les valeurs
      for (TreeNode node : subtree) {
        Value value = createValue(node);
        if (value != null) {
          PdcRightsDAO.deleteRights(con, axisId, value.getPK().getId());
        }
      }
      // Warning, we must update the path of the Value(Classify)
      if (!oldPath.isEmpty()) {
        // Après l'effacement de la valeur et de son arborescence, on recupere
        // les nouveaux chemins
        // Les nouveaux chemins sont tous identiques, c'est celui de la mère
        ArrayList<String> newPath = new ArrayList<>();
        for (int i = 0; i < oldPath.size(); i++) {
          newPath.add(motherPath);
        }
        pdcSubscriptionManager
            .checkValueOnDelete(Integer.parseInt(axisId), axisName, oldPath, newPath, pathInfo);

        // call the ClassifyBm to create oldValue and newValue
        // and to replace the oldValue by the newValue
        pdcClassifyManager.createValuesAndReplace(con, axisId, oldPath, newPath);
      }
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  /**
   * Delete the selected value. If a daughter of the selected value is named like a sister of her
   * mother the delete is not possible.
   * @param valueId - the id of the select value
   * @return null if the delete is possible, the name of her daughter else.
   */
  @Override
  public String deleteValue(Connection con, String valueId, String axisId, String treeId)
      throws PdcException {
    String possibleDaughterName;
    try {
      // first update any predefined classifications
      List<PdcAxisValue> valuesToDelete = new ArrayList<>();
      valuesToDelete.add(PdcAxisValue.aPdcAxisValue(valueId, axisId));
      pdcClassificationService.axisValuesDeleted(valuesToDelete);

      // then run the old legacy code about content classification on the PdC
      Value valueToDelete = getAxisValue(valueId, treeId);
      // filles de la mère = soeurs des filles de la valeur à supprimer
      List<Value> daughtersOfMother = getDaughters(con, valueToDelete.getMotherId(), treeId);
      // filles de la valeur à supprimer
      List<Value> daughtersOfValueToDelete = getDaughters(con, valueId, treeId);

      possibleDaughterName = isValueNameExist(daughtersOfMother, daughtersOfValueToDelete);
      if (possibleDaughterName == null) {

        // Mise à jour de la partie utilisation
        updateBaseValueInInstances(con, valueId, axisId, treeId);

        // Avant l'effacement de la valeur, on recupere les vieux chemins
        ArrayList<String> oldPath = getPathes(con, valueId, treeId);

        AxisHeader axisHeader = getAxisHeader(con, axisId);
        String axisName = axisHeader.getName();
        List<Value> pathInfo = getFullPath(valueId, treeId);

        treeService.deleteNode(con, new TreeNodePK(valueId), treeId);

        // on efface les droits sur la valeur
        PdcRightsDAO.deleteRights(con, axisId, valueId);

        // Warning, we must update the path of the Value(Classify)
        if (!oldPath.isEmpty()) {
          // Après l'effacement de la valeur, on creait les nouveaux chemins

          ArrayList<String> newPath = getPathes(con, valueId, treeId);
          // lecture de l'arrayList oldPath et on retire la valueId
          String pattern = "/" + valueId; // motif que l'on doit rechercher dans
          // l'ancien chemin pour le supprimer
          int lenOfPattern = pattern.length(); // longueur du motif
          int patternIdx; // position du pattern rechercher
          for (String path : oldPath) {
            patternIdx = path.indexOf(pattern); // ne peux etre à -1
            path = path.substring(0, patternIdx) +
                path.substring(patternIdx + lenOfPattern); // retire le motif
            if (path.split("/").length <= 2) {
              path = null;
            }
            newPath.add(path);
          }

          // call the ClassifyBm to create oldValue and newValue
          // and to replace the oldValue by the newValue
          pdcSubscriptionManager
              .checkValueOnDelete(Integer.parseInt(axisId), axisName, oldPath, newPath, pathInfo);
          pdcClassifyManager.createValuesAndReplace(con, axisId, oldPath, newPath);
        }
      }
    } catch (Exception e) {
      throw new PdcException(e);
    }
    return possibleDaughterName;
  }

  /**
   * Returns the full path of the value
   * @param valueId - the id of the selected value (value is not empty)
   * @return the complet path - It's a List of ArrayList. Each ArrayList contains the name, the id
   * and the treeId of the value in the path.
   */
  @Override
  public List<Value> getFullPath(String valueId, String treeId) throws PdcException {
    Connection con = openConnection();
    try {
      // récupère une collection de Value
      List<TreeNode> listTreeNode = treeService.getFullPath(con, new TreeNodePK(valueId), treeId);
      return createValuesList(listTreeNode);
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * This method searches if a name of axes is already used!
   * @param axis - a list of axes
   * @param axisToCheck - the axe to check its existence
   * @return true if the name of the axe exists, false otherwise
   */
  private boolean isAxisNameExist(List<AxisHeader> axis, AxisHeader axisToCheck) {
    String axisIdToCheck = axisToCheck.getPK().getId();
    String axisNameToCheck = axisToCheck.getName();

    boolean isExist = false; // by default, the name don't exist
    Iterator<AxisHeader> it = axis.iterator();
    AxisHeader axisHeader;

    while (it.hasNext()) {
      axisHeader = it.next();
      if (axisHeader.getName().equalsIgnoreCase(axisNameToCheck) &&
          !axisHeader.getPK().getId().equals(axisIdToCheck)) {
        isExist = true;
        break;
      }
    }

    return isExist;
  }

  /**
   * This method searches if a name of values is already used!
   * @param values - a list of values
   * @param valueToCheck - the value to check its existence
   * @return true if the name of the value exists, false otherwise
   */
  private boolean isValueNameExist(List<Value> values, Value valueToCheck) {
    String valueIdToCheck = valueToCheck.getPK().getId();
    String valueNameToCheck = valueToCheck.getName();
    boolean isExist = false; // by default, the name don't exist
    Iterator<Value> it = values.iterator();
    Value value;

    while (it.hasNext()) {
      value = it.next();
      if (value.getName().equalsIgnoreCase(valueNameToCheck) &&
          !value.getPK().getId().equals(valueIdToCheck)) {
        isExist = true;
        break;
      }
    }

    return isExist;
  }

  /**
   * This method searches if one name of valuesToCheck is alreadey used !
   * @param values - a list of values
   * @param valuesToCheck - a list of values to check
   * @return the name of the value if the name of one value exists, null otherwise
   */
  private String isValueNameExist(List<Value> values, List<Value> valuesToCheck) {
    Iterator<Value> it = valuesToCheck.iterator();
    Value valueToCheck;
    String valueName = null;
    while (it.hasNext()) {
      valueToCheck = it.next();
      if (isValueNameExist(values, valueToCheck)) {
        valueName = valueToCheck.getName();
        break;
      }
    }
    return valueName;
  }

  @Override
  public AxisHeader getAxisHeader(String axisId) {
    return getAxisHeader(axisId, true);
  }

  public AxisHeader getAxisHeader(String axisId, boolean setTranslations) {
    AxisHeader axisHeader = axisHeaders.get(axisId);
    if (axisHeader == null) {
      try {
        AxisHeaderPersistence axisHeaderPersistence = dao.findByPrimaryKey(new AxisPK(axisId));
        axisHeader = new AxisHeader(axisHeaderPersistence);

        axisHeaders.put(axisHeader.getPK().getId(), axisHeader);
      } catch (PersistenceException e) {
        SilverLogger.getLogger(this).silent(e);
      }
    }
    if (setTranslations && axisHeader != null) {
      setTranslations(axisHeader);
    }
    return axisHeader;
  }

  private AxisHeaderPersistence getAxisHeaderPersistence(String axisId) {
    try {
      return dao.findByPrimaryKey(new AxisPK(axisId));
    } catch (PersistenceException exSelect) {
      SilverLogger.getLogger(this).error("Failed to get headers for axis {0}",
          new String[]{axisId}, exSelect);
    }
    return null;
  }

  /**
   * Returns an AxisHeader Object. (pass the connection WORK AROUND FOR THE connection BUG !!!!!!!)
   * @param axisId - the id of the selected axe
   * @return an AxisHeader
   */
  private AxisHeader getAxisHeader(Connection connection, String axisId) {
    try {
      AxisHeaderPersistence axisHeaderPersistence =
          dao.findByPrimaryKey(connection, new AxisPK(axisId));
      return new AxisHeader(axisHeaderPersistence);
    } catch (PersistenceException exSelect) {
      throw new PdcRuntimeException(exSelect);
    }
  }

  /**
   * Returns a list of Value Object.
   * @param con - a connection
   * @param refValue - the id of the selected axe
   * @return a list
   */
  private List<Value> getDaughters(Connection con, String refValue, String treeId) {
    List<Value> daughters = new ArrayList<>();

    try {
      daughters =
          createValuesList(treeService.getSonsToNode(con, new TreeNodePK(refValue), treeId));
    } catch (Exception exList) {
      SilverLogger.getLogger(this).warn(exList.getMessage());
    }

    return daughters;
  }

  @Override
  public List<Value> getDaughters(String axisId, String valueId) {
    List<Value> daughters = new ArrayList<>();
    Connection con = null;
    try {
      con = openConnection();
      AxisHeader axisHeader = getAxisHeader(axisId, false);
      int tId = axisHeader.getRootId();
      return getAxisValues(tId);
    } catch (Exception e) {
      SilverLogger.getLogger(this).warn(e.getMessage());
    } finally {
      DBUtil.close(con);
    }
    return daughters;
  }

  /**
   * Creates a list of Value objects with a list of treeNodes objects
   * @param treeNodes - a list of TreeNode objects
   * @return a Value list
   */
  private List<Value> createValuesList(List<TreeNode> treeNodes) {
    List<Value> values = new ArrayList<>();

    for (TreeNode node : treeNodes) {
      values.add(createValue(node));
    }

    return values;
  }

  /**
   * Creates a Value object with a TreeNode object
   * @param treeNode - a TreeNode Object
   * @return a Value Object
   */
  private Value createValue(TreeNode treeNode) {
    if (treeNode != null) {
      Value value = new Value(treeNode);
      value.setTranslations(treeNode.getTranslations());
      return value;
    }
    return null;
  }

  /**
   * **************************************************
   * ******** PDC Utilization Settings Methods ********
   * **************************************************
   */
  @Override
  public UsedAxis getUsedAxis(String usedAxisId) throws PdcException {
    return pdcUtilizationService.getUsedAxis(usedAxisId);
  }

  @Override
  public List<UsedAxis> getUsedAxisByInstanceId(String instanceId) throws PdcException {
    return pdcUtilizationService.getUsedAxisByInstanceId(instanceId);
  }

  @Override
  public int addUsedAxis(UsedAxis usedAxis) throws PdcException {
    AxisHeader axisHeader = getAxisHeader(Integer.toString(usedAxis.getAxisId()), false); // get the
    // header
    // of the
    // axe to
    // obtain the treeId.
    String treeId = Integer.toString(axisHeader.getRootId());
    return pdcUtilizationService.addUsedAxis(usedAxis, treeId);
  }

  @Override
  public int updateUsedAxis(UsedAxis usedAxis) throws PdcException {
    AxisHeader axisHeader = getAxisHeader(Integer.toString(usedAxis.getAxisId()), false); // get the
    // header
    // of the
    // axe to
    // obtain the treeId.
    String treeId = Integer.toString(axisHeader.getRootId());

    // on recherche si la nouvelle valeur de base est une valeur ascendante à la valeur de base
    // originelle si c'est le cas alors on peut faire un update.
    // sinon, il faut vérifier qu'aucune valeur fille de cet axe n'est positionnée.
    // si une valeur fille est positionnée, on ne peut pas modifier la valeur de base du UsedAxis
    // on récupère la valeur de base que l'on veut modifier de l'objet UsedAxis
    String id = usedAxis.getPK().getId();
    UsedAxis currentUsedAxis = pdcUtilizationService.getUsedAxis(id);

    // on récupère la liste des objets pour une instance de jobPeas donnée
    List<Integer> objectIdList = pdcClassifyManager.getObjectsByInstance(usedAxis.getInstanceId());

    // on vérifie d'abord que la nouvelle valeur de base est une valeur ascendante de la valeur
    // de base que l'on souhaite modifié
    if (!objectIdList.isEmpty() && !isAscendanteBaseValue(objectIdList, usedAxis)) {
      // la nouvelle valeur de base est soit une valeur d'un autre axe soit une valeur fille de
      // la valeur de base que l'on veut modifier on vérifie que l'axe courant n'a pas de
      // documents positionnés
      if (pdcClassifyManager.hasAlreadyPositions(objectIdList, currentUsedAxis)) {
        return 2;
      } else {
        return pdcUtilizationService.updateUsedAxis(usedAxis, treeId);
      }
    } else {
      // la nouvelle valeur de base est ascendante. On peut donc modifier
      return pdcUtilizationService.updateUsedAxis(usedAxis, treeId);
    }
  }

  /**
   * recherche si la valeur de base de l'axe est une valeur ascendante par rapport aux valeurs se
   * trouvant dans SB_Classify...
   * @param objectIdList - une list d'objets se trouvant dans une instance donnée
   * @param usedAxis - l'objet UsedAxis contenant la nouvelle valeur de base
   * @return vrai si la valeur de base est une valeur ascendante sinon faux
   */
  private boolean isAscendanteBaseValue(List<Integer> objectIdList, UsedAxis usedAxis)
      throws PdcException {
    return pdcClassifyManager.hasAlreadyPositions(objectIdList, usedAxis);
  }

  /**
   * Update a base value from the PdcUtilization table
   */
  private void updateBaseValueInInstances(Connection con, String baseValueToUpdate, String axisId,
      String treeId) throws PdcException {

    // recherche la valeur mère de baseValueToUpdate
    Value value = getAxisValue(baseValueToUpdate, treeId);
    int newBaseValue = Integer.parseInt(value.getMotherId());
    pdcUtilizationService
        .updateOrDeleteBaseValue(con, Integer.parseInt(baseValueToUpdate), newBaseValue,
            Integer.parseInt(axisId), treeId);
  }

  /**
   * Update some base values from the PdcUtilization table
   */
  private void updateBaseValuesInInstances(Connection con, String baseValueToUpdate, String axisId,
      String treeId) throws PdcException {

    List<TreeNode> descendants;

    try {
      descendants = treeService.getSubTree(con, new TreeNodePK(baseValueToUpdate), treeId);
    } catch (Exception e) {
      throw new PdcException(e);
    }

    // recherche la valeur mère de baseValueToUpdate
    Value value = getAxisValue(baseValueToUpdate, treeId);
    int newBaseValue = Integer.parseInt(value.getMotherId());
    String descendantId;
    for (TreeNode descendant : descendants) {
      descendantId = descendant.getPK().getId();
      pdcUtilizationService
          .updateOrDeleteBaseValue(con, Integer.parseInt(descendantId), newBaseValue,
              Integer.parseInt(axisId), treeId);
    }
  }

  @Override
  public void deleteUsedAxis(String usedAxisId) throws PdcException {
    pdcUtilizationService.deleteUsedAxis(usedAxisId);
  }

  /*
   * *********************************************
   * ******** PDC CLASSIFY METHODS ***************
   * *********************************************
   */
  @Override
  public List<UsedAxis> getUsedAxisToClassify(String instanceId, int silverObjectId)
      throws PdcException {
    List<UsedAxis> usedAxis = getUsedAxisByInstanceId(instanceId);
    if (usedAxis.isEmpty()) {
      fillUsedAxis(instanceId, usedAxis);
    } else {
      initUsedAxis(instanceId, silverObjectId, usedAxis);
    }
    return usedAxis;
  }

  private void initUsedAxis(final String instanceId, final int silverObjectId,
      final List<UsedAxis> usedAxis) throws PdcException {
    for (UsedAxis axis : usedAxis) {
      if (I18NHelper.isI18nContentActivated) {
        AxisHeader header = getAxisHeader(Integer.toString(axis.getAxisId()));
        axis._setAxisHeader(header);
      }
      int axisRootId = axis._getAxisRootId();
      axis._setAxisValues(getAxisValues(axisRootId));
      if (axis.getVariant() == 0 && silverObjectId >= 0) {
        // Si l'axe est invariant, il faut préciser la valeur obligatoire
        List<ClassifyPosition> positions = getPositions(silverObjectId, instanceId);
        String invariantValue;
        if (!positions.isEmpty()) {
          for (ClassifyPosition position : positions) {
            invariantValue = position.getValueOnAxis(axis.getAxisId());
            axis._setInvariantValue(invariantValue);
          }
        }
      }
    }
  }

  private void fillUsedAxis(final String instanceId, final List<UsedAxis> usedAxis)
      throws PdcException {
    List<AxisHeader> headers = getAxis();
    for (AxisHeader axisHeader : headers) {
      UsedAxis axis =
          new UsedAxis(axisHeader.getPK().getId(), instanceId, axisHeader.getRootId(), 0, 0, 1);
      axis._setAxisHeader(axisHeader);
      axis._setAxisName(axisHeader.getName());
      axis._setAxisType(axisHeader.getAxisType());
      axis._setBaseValueName(axisHeader.getName());
      axis._setAxisRootId(axisHeader.getRootId());
      axis._setAxisValues(getAxisValues(axisHeader.getRootId()));
      usedAxis.add(axis);
    }
  }

  @Override
  public void addPositions(List<ClassifyPosition> positions, int objectId, String instanceId)
      throws PdcException {
    List<UsedAxis> usedAxis = getUsedAxisByInstanceId(instanceId);

    for (ClassifyPosition position : positions) {
      ClassifyPosition newPosition = checkClassifyPosition(position, usedAxis);
      if (newPosition != null) {
        // copy position
        addPosition(objectId, newPosition, instanceId);
      }
    }
  }

  @Override
  public void copyPositions(int fromObjectId, String fromInstanceId, int toObjectId,
      String toInstanceId) throws PdcException {
    List<ClassifyPosition> positions = getPositions(fromObjectId, fromInstanceId);
    List<ClassifyPosition> existingPositions = getPositions(toObjectId, toInstanceId);

    List<UsedAxis> usedAxis = getUsedAxisByInstanceId(toInstanceId);

    for (ClassifyPosition position : positions) {
      if (!isYetSet(existingPositions, position)) {
        ClassifyPosition newPosition = checkClassifyPosition(position, usedAxis);

        if (newPosition != null) {
          // copy position
          addPosition(toObjectId, newPosition, toInstanceId);
        }
      }
    }
  }

  private boolean isYetSet(List<ClassifyPosition> positions, ClassifyPosition position) {
    return positions.stream().anyMatch(p -> p.equals(position));
  }

  private ClassifyPosition checkClassifyPosition(ClassifyPosition position,
      List<UsedAxis> usedAxis) {
    ClassifyPosition newPosition = new ClassifyPosition();

    List<ClassifyValue> values = position.getListClassifyValue();
    for (ClassifyValue value : values) {
      value = checkClassifyValue(value, usedAxis);
      if (value != null) {
        newPosition.addValue(value);
      }
    }

    if (newPosition.getValues() == null) {
      return null;
    }

    return newPosition;
  }

  private ClassifyValue checkClassifyValue(ClassifyValue value, List<UsedAxis> usedAxis) {
    UsedAxis uAxis = getUsedAxis(usedAxis, value.getAxisId());
    if (uAxis == null) {
      // This axis is not used by the instance
      return null;
    } else {
      // Check base value
      String baseValuePath = uAxis._getBaseValuePath();
      if (!("/" + value.getValue() + "/").contains(baseValuePath)) {
        return null;
      }
    }
    return value;
  }

  /**
   * From the usedAxis, retrieve the UsedAxis corresponding to axisId
   * @param usedAxis a List of UsedAxis
   * @param axisId the axis id to search
   * @return the UsedAxis found or null if no object found
   */
  private UsedAxis getUsedAxis(List<UsedAxis> usedAxis, int axisId) {
    Iterator<UsedAxis> iterator = usedAxis.iterator();
    UsedAxis uAxis;
    while (iterator.hasNext()) {
      uAxis = iterator.next();
      if (uAxis.getAxisId() == axisId) {
        return uAxis;
      }
    }
    return null;
  }

  @Override
  public int addPosition(int silverObjectId, ClassifyPosition position, String sComponentId)
      throws PdcException {
    return addPosition(silverObjectId, position, sComponentId, true);
  }

  @Override
  public int addPosition(int silverObjectId, ClassifyPosition position, String sComponentId,
      boolean alertSubscribers) throws PdcException {
    // First check if the object is already classified on the position
    int positionId = pdcClassifyManager.isPositionAlreadyExists(silverObjectId, position);

    if (positionId == -1) {
      // The position doesn't exists. We add it.
      positionId = pdcClassifyManager.addPosition(silverObjectId, position, sComponentId);

      if (alertSubscribers) {
        // Alert subscribers to the position
        pdcSubscriptionManager
            .checkSubscriptions(position.getValues(), sComponentId, silverObjectId);
      }
    }

    return positionId;
  }

  @Override
  public int updatePosition(ClassifyPosition position, String instanceId, int silverObjectId)
      throws PdcException {
    return updatePosition(position, instanceId, silverObjectId, true);
  }

  @Override
  public int updatePosition(ClassifyPosition position, String instanceId, int silverObjectId,
      boolean alertSubscribers) throws PdcException {

    List<UsedAxis> usedAxisList = getUsedAxisToClassify(instanceId, silverObjectId);
    Set<Integer> invariantUsedAxis = new HashSet<>(usedAxisList.size());
    for (UsedAxis ua : usedAxisList) {
      // on cherche les axes invariants
      if (ua.getVariant() == 0) {
        invariantUsedAxis.add(ua.getAxisId());
      }
    }

    // maintenant, on cherche les valeurs qui sont sur un axe invariant
    List<ClassifyValue> classifyValueList = position.getValues();
    List<org.silverpeas.core.pdc.classification.Value> classifyValues = new ArrayList<>();
    for (ClassifyValue cv : classifyValueList) {
      if (invariantUsedAxis.contains(cv.getAxisId())) {
        classifyValues.add(cv);
      }
    }

    pdcClassifyManager.updatePosition(position);

    // on update les axes invariants
    if (!classifyValues.isEmpty()) {
      pdcClassifyManager.updatePositions(classifyValues, silverObjectId);
    }

    if (alertSubscribers) {
      pdcSubscriptionManager.checkSubscriptions(position.getValues(), instanceId, silverObjectId);
    }

    return 0;
  }

  @Override
  public void deletePosition(int positionId, String sComponentId) throws PdcException {
    pdcClassifyManager.deletePosition(positionId, sComponentId);
  }

  @Override
  public List<ClassifyPosition> getPositions(int silverObjectId, String sComponentId)
      throws PdcException {
    List<Position<org.silverpeas.core.pdc.classification.Value>> positions =
        pdcClassifyManager.getPositions(silverObjectId, sComponentId);
    ArrayList<ClassifyPosition> classifyPositions = new ArrayList<>();

    // transform Position to ClassifyPosition
    ClassifyPosition classifyPosition;
    for (Position<org.silverpeas.core.pdc.classification.Value> position : positions) {
      List<org.silverpeas.core.pdc.classification.Value> values = position.getValues();

      // transform Value to ClassifyValue
      ArrayList<ClassifyValue> classifyValues = new ArrayList<>();
      for (org.silverpeas.core.pdc.classification.Value value : values) {
        ClassifyValue classifyValue = new ClassifyValue(value.getAxisId(), value.getValue());

        if (value.getAxisId() != -1) {
          int treeId = Integer.parseInt(getTreeId(Integer.toString(value.getAxisId())));
          // enrichit le classifyValue avec le chemin complet de la racine jusqu'à la valeur
          String valuePath = value.getValue();
          if (valuePath != null) {
            // enleve le dernier /
            valuePath = valuePath.substring(0, valuePath.length() - 1);
            String valueId = valuePath.substring(valuePath.lastIndexOf('/') + 1);
            classifyValue.setFullPath(getFullPath(valueId, String.valueOf(treeId)));
            classifyValues.add(classifyValue);
          }
        }
      }

      classifyPosition = new ClassifyPosition(classifyValues);
      classifyPosition.setPositionId(position.getPositionId());
      classifyPositions.add(classifyPosition);
    }
    return classifyPositions;
  }

  // recherche à l'intérieur d'une instance
  @Override
  public List<SearchAxis> getPertinentAxisByInstanceId(SearchContext searchContext, String axisType,
      String instanceId) throws PdcException {
    List<String> instanceIds = new ArrayList<>();
    instanceIds.add(instanceId);
    return getPertinentAxisByInstanceIds(searchContext, axisType, instanceIds);
  }

  // recherche à l'intérieur d'une liste d'instance
  @Override
  public List<SearchAxis> getPertinentAxisByInstanceIds(SearchContext searchContext,
      String axisType, List<String> instanceIds) throws PdcException {
    List<AxisHeader> axis =
        pdcUtilizationService.getAxisHeaderUsedByInstanceIds(instanceIds);
    ArrayList<Integer> axisIds = new ArrayList<>();
    String axisId;
    for (AxisHeader axisHeader : axis) {
      if (axisHeader.getAxisType().equals(axisType)) {
        axisId = axisHeader.getPK().getId();
        axisIds.add(Integer.parseInt(axisId));
      }
    }

    List<PertinentAxis> pertinentAxis = pdcClassifyManager.getPertinentAxis(searchContext, axisIds,
        instanceIds);
    return transformPertinentAxisIntoSearchAxis(pertinentAxis, axis);
  }

  private List<SearchAxis> transformPertinentAxisIntoSearchAxis(
      List<PertinentAxis> pertinentAxisList, List<AxisHeader> axis) throws PdcException {
    List<SearchAxis> searchAxisList = new ArrayList<>();
    SearchAxis searchAxis;
    String axisId;
    for (PertinentAxis pertinentAxis : pertinentAxisList) {
      axisId = Integer.toString(pertinentAxis.getAxisId());
      searchAxis = new SearchAxis(pertinentAxis.getAxisId(), pertinentAxis.getNbObjects());
      for (AxisHeader axisHeader : axis) {
        if (axisHeader.getPK().getId().equals(axisId)) {
          setTranslations(axisHeader);

          searchAxis.setAxis(axisHeader);
          searchAxis.setAxisRootId(Integer.parseInt(getRootId(axisHeader.getPK().getId())));
          searchAxisList.add(searchAxis);
        }
      }
    }
    return searchAxisList;
  }

  // recherche à l'intérieur d'une instance
  @Override
  public List<Value> getPertinentDaughterValuesByInstanceId(SearchContext searchContext,
      String axisId, String valueId, String instanceId) throws PdcException {
    List<String> instanceIds = new ArrayList<>();
    instanceIds.add(instanceId);
    return getPertinentDaughterValuesByInstanceIds(searchContext, axisId, valueId, instanceIds);
  }

  // recherche à l'intérieur d'une liste d'instance
  @Override
  public List<Value> getPertinentDaughterValuesByInstanceIds(SearchContext searchContext,
      String axisId, String valueId, List<String> instanceIds) throws PdcException {
    return filterValues(searchContext, axisId, instanceIds);
  }

  @Override
  public List<Value> getFirstLevelAxisValuesByInstanceId(SearchContext searchContext, String axisId,
      String instanceId) throws PdcException {
    List<String> instanceIds = new ArrayList<>();
    instanceIds.add(instanceId);
    return getFirstLevelAxisValuesByInstanceIds(searchContext, axisId, instanceIds);
  }

  @Override
  public List<Value> getFirstLevelAxisValuesByInstanceIds(SearchContext searchContext,
      String axisId, List<String> instanceIds) throws PdcException {
    getRootId(axisId);
    return filterValues(searchContext, axisId, instanceIds);
  }

  private String getRootId(String axisId) throws PdcException {
    final String rootId;
    try (final Connection con = openConnection()) {
      AxisHeader axisHeader = getAxisHeader(axisId, false); // get the header of
      // the axe to obtain
      // the rootId.
      int treeId = axisHeader.getRootId();
      TreeNode root = treeService.getRoot(con, Integer.toString(treeId));
      rootId = root.getPK().getId();
    } catch (Exception e) {
      throw new PdcException(e);
    }
    return rootId;
  }

  private List<Value> filterValues(SearchContext searchContext, String axisId,
      List<String> instanceIds) throws PdcException {
    ArrayList<String> emptyValues = new ArrayList<>();

    // get the header of the axe to obtain the treeId.
    AxisHeader axisHeader = getAxisHeader(axisId, false);
    int treeId = axisHeader.getRootId();

    List<ObjectValuePair> allObjectValuePairs = pdcClassifyManager
        .getObjectValuePairs(searchContext, Integer.parseInt(axisId), instanceIds);

    // filter objects according to user rights
    List<ObjectValuePair> objectValuePairs =
        filterAvailableContents(allObjectValuePairs, searchContext.getUserId());

    // Get all the values for this treeService
    List<Value> descendants = getAxisValues(treeId);

    // Set the NbObject for all the pertinent values
    int nI = -1;
    while (++nI < descendants.size()) {
      // Get the i descendant
      Value descendant = descendants.get(nI);
      String descendantPath = descendant.getFullPath();

      // check if it's a leaf or not
      if (isLeaf(nI, descendants, descendant)) {
        // C'est une feuille, est-ce une feuille pertinente ?
        int nbContents = getNumberOfContents(objectValuePairs, descendantPath, false);
        if (nbContents > 0) {
          descendant.setNbObjects(nbContents);
        } else {
          // Cette feuille n'est pas pertinente
          emptyValues.add(descendantPath);
          descendants.remove(nI--);
        }
      } else {
        nI = processEmptyValues(emptyValues, descendantPath, descendants, nI, objectValuePairs,
            descendant);
      }
    }

    return descendants;

  }

  private int processEmptyValues(ArrayList<String> emptyValues, String descendantPath,
      List<Value> descendants, int nI, List<ObjectValuePair> objectValuePairs, Value descendant) {
    // OPTIMIZATION : Checks if it is a descendant of an empty value
    boolean isEmpty = false;
    String emptyPath;
    for (int nJ = 0; nJ < emptyValues.size() && !isEmpty; nJ++) {
      emptyPath = emptyValues.get(nJ);
      if (descendantPath.startsWith(emptyPath)) {
        isEmpty = true;
      }
    }

    // Set the real number of objects or remove the empty values
    if (isEmpty) {
      descendants.remove(nI--);
    } else {
      int nbObjects = getNumberOfContents(objectValuePairs, descendantPath, true);
      if (nbObjects > 0) {
        descendant.setNbObjects(nbObjects);
      } else {
        emptyValues.add(descendantPath);
        descendants.remove(nI--);
      }
    }
    return nI;
  }

  private static boolean isLeaf(int nI, List<Value> descendants, Value descendant) {
    boolean isLeaf;
    if (nI + 1 < descendants.size()) {
      Value nextDescendant = descendants.get(nI + 1);
      if (nextDescendant != null) {
        isLeaf = (nextDescendant.getLevelNumber() <= descendant.getLevelNumber());
      } else {
        isLeaf = false;
      }
    } else {
      isLeaf = true;
    }
    return isLeaf;
  }

  private List<ObjectValuePair> filterAvailableContents(List<ObjectValuePair> ovps, String userId) {
    final List<PublicationPK> pks = ovps.stream()
        .filter(o -> o.getInstanceId().startsWith(KMELIA_COMPONENT_NAME))
        .map(o -> new PublicationPK(o.getObjectId(), o.getInstanceId()))
        .collect(toList());
    final AccessControlContext context = AccessControlContext.init().onOperationsOf(SEARCH);
    final Set<String> accessiblePks = PublicationAccessControl.get()
        .filterAuthorizedByUser(pks, userId, context)
        .map(p -> p.getInstanceId() + "@" + p.getId())
        .collect(Collectors.toSet());
    return ovps.stream()
        .filter(o -> !o.getInstanceId().startsWith(KMELIA_COMPONENT_NAME) || accessiblePks.contains(o.getInstanceId() + "@" + o.getObjectId()))
        .collect(toList());
  }

  private int getNumberOfContents(List<ObjectValuePair> ovps, String valuePath, boolean deeply) {
    int nb = 0;
    final Set<String> countedObjects = new HashSet<>(ovps.size());
    for (ObjectValuePair ovp : ovps) {
      String key = ovp.getInstanceId() + "-" + ovp.getObjectId();
      if ((deeply ? ovp.getValuePath().startsWith(valuePath) :
          ovp.getValuePath().equals(valuePath)) && !countedObjects.contains(key)) {
        nb++;
        countedObjects.add(key);
      }
    }
    return nb;
  }

  /**
   * To know if classifying is mandatory on a given component
   * @param componentId - id of the component to test
   * @return true if at least one axis has been selected on component AND at least one axis is
   * mandatory
   * @throws PdcException if an error occurs.
   */
  @Override
  public boolean isClassifyingMandatory(String componentId) throws PdcException {
    List<UsedAxis> axisUsed = getUsedAxisByInstanceId(componentId);
    if (axisUsed != null) {
      for (UsedAxis axis : axisUsed) {
        if (axis.getMandatory() == 1) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void indexAllAxis() throws PdcException {

    // starting by remove all indexed data relative to pdc
    IndexEngineProxy.removeScopedIndexEntries("pdc");

    Connection con = openConnection();
    try {
      for (AxisHeader a : getAxis()) {
        treeService.indexTree(con, a.getRootId());
      }
    } catch (Exception e) {
      throw new PdcException(e);
    } finally {
      DBUtil.close(con);
    }
  }

  /**
   * open connection
   */
  private Connection openConnection() throws PdcException {
    Connection con;
    try {
      con = DBUtil.openConnection();
    } catch (Exception e) {
      throw new PdcException(e);
    }
    return con;
  }

  private Connection openTransaction() throws PdcException {
    Connection con = null;
    try {
      con = DBUtil.openConnection();
      con.setAutoCommit(false);
    } catch (Exception e) {
      DBUtil.close(con);
      throw new PdcException(e);
    }
    return con;
  }

  /**
   * @param con the connection to rollback
   */
  private void rollbackTransaction(Connection con) {
    if (con != null) {
      try {
        con.rollback();
      } catch (Exception e) {
        SilverLogger.getLogger(this).error(e.getMessage(), e);
      }
    }
  }

  /**
   * @param con the connection to commit
   */
  private void commitTransaction(Connection con) {
    if (con != null) {
      try {
        con.commit();
      } catch (Exception e) {
        SilverLogger.getLogger(this).error(e.getMessage(), e);
      }
    }
  }

  /*
   * *********************************************
   * ******** CONTAINER INTERFACE METHODS ********
   * *********************************************
   */

  /**
   * Get the SearchContext of the first position for the given SilverContentId
   */
  @Override
  public SearchContext getSilverContentIdSearchContext(int nSilverContentId,
      String sComponentId) throws PdcException {
    try {
      // Get the positions
      List<Position<org.silverpeas.core.pdc.classification.Value>> alPositions =
          pdcClassifyManager.getPositions(nSilverContentId, sComponentId);

      // Convert the first position in SearchContext
      SearchContext searchContext = new SearchContext(null);
      if (alPositions != null && !alPositions.isEmpty()) {
        Position<org.silverpeas.core.pdc.classification.Value> pos = alPositions.get(0);
        List<org.silverpeas.core.pdc.classification.Value> alValues = pos.getValues();
        for (int nI = 0; alValues != null && nI < alValues.size(); nI++) {
          org.silverpeas.core.pdc.classification.Value value = alValues.get(nI);
          if (value.getAxisId() != -1 && value.getValue() != null) {
            searchContext.addCriteria(new SearchCriteria(value.getAxisId(), value.getValue()));
          }
        }
      }

      return searchContext;
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  @Override
  public List<Integer> findSilverContentIdByPosition(SearchContext containerPosition,
      List<String> alComponentId, String authorId, LocalDate afterDate, LocalDate beforeDate)
      throws PdcException {
    return findSilverContentIdByPosition(containerPosition, alComponentId, authorId, afterDate,
        beforeDate, true, true);
  }

  /**
   * Find all the SilverContentId with the given position
   */
  private List<Integer> findSilverContentIdByPosition(SearchContext containerPosition,
      List<String> alComponentId, String authorId, LocalDate afterDate, LocalDate beforeDate,
      boolean recursiveSearch, boolean visibilitySensitive) throws PdcException {
    try {
      // Get the objects
      return pdcClassifyManager
          .findSilverContentIdByPosition(containerPosition, alComponentId, authorId,
              DateUtil.formatDate(afterDate),
              DateUtil.formatDate(beforeDate), recursiveSearch, visibilitySensitive);
    } catch (Exception e) {
      throw new PdcException(e);
    }
  }

  @Override
  public List<Integer> findSilverContentIdByPosition(SearchContext containerPosition,
      List<String> alComponentId) throws PdcException {
    return findSilverContentIdByPosition(containerPosition, alComponentId, true, true);
  }

  private List<Integer> findSilverContentIdByPosition(SearchContext containerPosition,
      List<String> alComponentId, boolean recursiveSearch, boolean visibilitySensitive)
      throws PdcException {
    return findSilverContentIdByPosition(containerPosition, alComponentId, null, null, null,
        recursiveSearch, visibilitySensitive);
  }

  private List<PdcAxisValue> findRecursivelyAllChildrenOf(final PdcAxisValue axisValue) {
    Set<PdcAxisValue> directChildrenOfValue = axisValue.getChildValues();
    List<PdcAxisValue> allChildren = new ArrayList<>(directChildrenOfValue);
    for (PdcAxisValue aChild : directChildrenOfValue) {
      allChildren.addAll(findRecursivelyAllChildrenOf(aChild));
    }
    return allChildren;
  }

  @Override
  public List<GlobalSilverContent> getSilverContentsByIds(List<Integer> silverContentIds,
      String userId) {
    if (isEmpty(silverContentIds)) {
      return emptyList();
    }
    try {
      final Map<String, GlobalSilverContentProcessor> processors = ServiceProvider
          .getAllServices(GlobalSilverContentProcessor.class).stream()
          .collect(toMap(GlobalSilverContentProcessor::relatedToComponent, p -> p));
      return contentMgtEngine.getResourceReferencesByContentIds(silverContentIds).stream()
          .collect(groupingBy(r -> getComponentName(r.getComponentInstanceId()),
              mapping(r -> r, toList())))
          .entrySet().stream()
          .flatMap(e -> {
            final String componentName = e.getKey();
            final List<ResourceReference> references = e.getValue();
            try {
              final ContentPeas contentP =
                  contentMgtEngine.getContentPeasByComponentName(componentName);
              if (contentP != null) {
                // we are going to search only SilverContent of this instanceId
                final SilverpeasContentManager contentManager = contentP.getContentManager();
                //noinspection
                final var localSilverContents =
                    contentManager.getSilverContentByReference(references, userId);
                if (localSilverContents != null) {
                  final GlobalSilverContentProcessor processor =
                      getGlobalSilverContentProcessor(processors, componentName);
                  return processor.asGlobalSilverContent(localSilverContents);
                }
              }
            } catch (Exception ex) {
              SilverLogger.getLogger(this)
                  .error("Can't retrieve content from taxonomy for component {0}",
                      new String[]{componentName}, ex);
            }
            return Stream.empty();
          })
          .collect(toList());
    } catch (Exception e) {
      throw new PdcRuntimeException(e);
    }
  }

  private GlobalSilverContentProcessor getGlobalSilverContentProcessor(
      final Map<String, GlobalSilverContentProcessor> processors, final String c) {
    GlobalSilverContentProcessor processor = processors.get(c);
    if (processor == null) {
      processor = processors.get("default");
    }
    return processor;
  }
}