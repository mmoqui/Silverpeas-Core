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
package org.silverpeas.core.pdc.tree.service;

import org.silverpeas.core.annotation.Repository;
import org.silverpeas.core.pdc.tree.model.TreeNodeI18N;
import org.silverpeas.core.persistence.jdbc.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class declaration
 * @author
 */
@Repository
public class TreeI18NDAO {

  private static final String DELETE_FROM = "delete from ";
  private static String treeI18NTable = "SB_Tree_TreeI18N";

  private static final String COLUMNS = "id,treeId,nodeId,lang,name,description";

  /**
   * Constructor declaration
   *
   */
  protected TreeI18NDAO() {

  }

  public List<TreeNodeI18N> getTranslations(Connection con, String treeId, String nodeId)
      throws SQLException {
    String selectQuery = "select * from " + treeI18NTable + " where treeId = ? and nodeId = ?";
    List<TreeNodeI18N> allTranslations = new ArrayList<>();

    PreparedStatement prepStmt = null;
    ResultSet rs = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setInt(1, Integer.parseInt(treeId));
      prepStmt.setInt(2, Integer.parseInt(nodeId));

      rs = prepStmt.executeQuery();

      TreeNodeI18N translation = null;
      while (rs.next()) {
        translation = new TreeNodeI18N();
        translation.setId(String.valueOf(rs.getInt(1)));
        translation.setObjectId(Integer.toString(rs.getInt(3)));
        translation.setLanguage(rs.getString(4));
        translation.setName(rs.getString(5));
        translation.setDescription(rs.getString(6));

        allTranslations.add(translation);
      }
    } finally {
      DBUtil.close(rs, prepStmt);
    }

    return allTranslations;
  }

  public void createTranslation(Connection con, TreeNodeI18N translation,
      String treeId) throws SQLException {
    String selectQuery = "insert into " + treeI18NTable + "(" + COLUMNS
        + ") values  (?, ?, ?, ?, ?, ?)";
    PreparedStatement prepStmt = null;
    int id = -1;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      id = DBUtil.getNextId(treeI18NTable, "id");
      prepStmt.setInt(1, id);
      prepStmt.setInt(2, Integer.parseInt(treeId));
      prepStmt.setInt(3, Integer.parseInt(translation.getObjectId()));
      prepStmt.setString(4, translation.getLanguage());
      prepStmt.setString(5, translation.getName());
      prepStmt.setString(6, translation.getDescription());

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(prepStmt);
    }
  }

  public void updateTranslation(Connection con, TreeNodeI18N translation) throws SQLException {
    String selectQuery = "update " + treeI18NTable + " set name = ?, description = ? where id = ?";
    PreparedStatement prepStmt = null;
    ResultSet rs = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setString(1, translation.getName());
      prepStmt.setString(2, translation.getDescription());
      prepStmt.setInt(3, Integer.parseInt(translation.getId()));

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(rs, prepStmt);
    }
  }

  public void updateTranslation(Connection con, String treeId, TreeNodeI18N translation)
      throws SQLException {
    String selectQuery = "update " + treeI18NTable +
        " set name = ?, description = ? where treeId = ? and nodeId = ? and lang = ? ";
    PreparedStatement prepStmt = null;
    ResultSet rs = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setString(1, translation.getName());
      prepStmt.setString(2, translation.getDescription());
      prepStmt.setInt(3, Integer.parseInt(treeId));
      prepStmt.setInt(4, Integer.parseInt(translation.getObjectId()));
      prepStmt.setString(5, translation.getLanguage());

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(rs, prepStmt);
    }
  }

  public void deleteTranslation(Connection con, String translationId) throws SQLException {
    String selectQuery = DELETE_FROM + treeI18NTable + " where id = ?";
    PreparedStatement prepStmt = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setInt(1, Integer.parseInt(translationId));

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(prepStmt);
    }
  }

  public void deleteTranslation(Connection con, String treeId, String nodeId, String language)
      throws SQLException {
    String selectQuery =
        DELETE_FROM + treeI18NTable + " where treeId = ? and nodeId = ? and lang = ? ";
    PreparedStatement prepStmt = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setInt(1, Integer.parseInt(treeId));
      prepStmt.setInt(2, Integer.parseInt(nodeId));
      prepStmt.setString(3, language);

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(prepStmt);
    }
  }

  public void deleteTreeTranslations(Connection con, String treeId) throws SQLException {
    String selectQuery = DELETE_FROM + treeI18NTable + " where treeId = ?";
    PreparedStatement prepStmt = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setInt(1, Integer.parseInt(treeId));

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(prepStmt);
    }
  }

  public void deleteNodeTranslations(Connection con, String treeId, String nodeId)
      throws SQLException {
    String selectQuery = DELETE_FROM + treeI18NTable + " where treeId = ? and nodeId = ?";
    PreparedStatement prepStmt = null;
    try {
      prepStmt = con.prepareStatement(selectQuery);
      prepStmt.setInt(1, Integer.parseInt(treeId));
      prepStmt.setInt(2, Integer.parseInt(nodeId));

      prepStmt.executeUpdate();
    } finally {
      DBUtil.close(prepStmt);
    }
  }

}