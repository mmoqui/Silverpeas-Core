<%--

    Copyright (C) 2000 - 2024 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have received a copy of the text describing
    the FLOSS exception, and it is also available here:
    "https://www.silverpeas.org/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

--%>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
response.setHeader("Cache-Control","no-store"); //HTTP 1.1
response.setHeader("Pragma","no-cache");        //HTTP 1.0
response.setDateHeader ("Expires",-1);          //prevents caching at the proxy server
%>

<%@ page import="org.silverpeas.core.admin.component.model.ComponentInst"%>
<%@ page import="org.silverpeas.core.admin.component.model.LocalizedGroupOfParameters"%>
<%@ page import="org.silverpeas.core.admin.component.model.LocalizedOption"%>
<%@ page import="org.silverpeas.core.admin.component.model.LocalizedParameter"%>
<%@ page import="org.silverpeas.core.admin.component.model.WAComponent"%>

<%// En fonction de ce dont vous avez besoin %>
<%@ page import="org.silverpeas.core.admin.space.SpaceInst"%>
<%@ page import="org.silverpeas.core.admin.user.model.ProfileInst"%>
<%@ page import="org.silverpeas.core.util.MultiSilverpeasBundle"%>
<%@ page import="org.silverpeas.kernel.bundle.ResourceLocator"%>
<%@ page import="org.silverpeas.kernel.util.StringUtil"%>
<%@ page import="org.silverpeas.core.util.URLUtil"%>
<%@ page import="org.silverpeas.core.util.WebEncodeHelper"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.GraphicElementFactory"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.arraypanes.ArrayCellText"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.arraypanes.ArrayColumn"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.arraypanes.ArrayLine"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.arraypanes.ArrayPane"%>

<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.board.Board"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.browsebars.BrowseBar"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.buttonpanes.ButtonPane"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.buttons.Button"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.frame.Frame"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.iconpanes.IconPane"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.icons.Icon"%>


<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.operationpanes.OperationPane"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.tabs.TabbedPane"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.window.Window"%>
<%@ page import="org.silverpeas.web.jobstartpage.AllComponentParameters"%>

<%@ page import="org.silverpeas.web.jobstartpage.DisplaySorted"%>
<%@ page import="org.silverpeas.web.jobstartpage.JobStartPagePeasSettings"%>
<%@ page import="org.silverpeas.web.jobstartpage.SpaceLookHelper" %>
<%@ page import="org.silverpeas.web.jobstartpage.control.JobStartPagePeasSessionController" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>

<%@ page errorPage="../../admin/jsp/errorpageMain.jsp"%>

<%@ taglib uri="http://www.silverpeas.com/tld/viewGenerator" prefix="view"%>

<%
GraphicElementFactory gef = (GraphicElementFactory) session.getAttribute("SessionGraphicElementFactory");
JobStartPagePeasSessionController jobStartPageSC = (JobStartPagePeasSessionController) request.getAttribute("jobStartPagePeas");

String iconsPath = ResourceLocator.getGeneralSettingBundle().getString("ApplicationURL");
String m_context = iconsPath;

Boolean haveToRefreshNavBar = (Boolean)request.getAttribute("haveToRefreshNavBar");

MultiSilverpeasBundle resource = (MultiSilverpeasBundle)request.getAttribute("resources");
Window window = gef.getWindow();
BrowseBar browseBar = window.getBrowseBar();
browseBar.setSpaceJavascriptCallback("parent.jumpToSpace");
browseBar.setComponentJavascriptCallback("parent.jumpToComponent");
OperationPane operationPane = window.getOperationPane();
Frame frame = gef.getFrame();
Board board = gef.getBoard();
%>
<% if (haveToRefreshNavBar != null && haveToRefreshNavBar) {
  boolean isRoot = true;
  String currentSpaceId = "undefined";
  SpaceInst currentSpace = jobStartPageSC.getSpaceInstById();
  if (currentSpace != null) {
    isRoot = currentSpace.isRoot();
    currentSpaceId = "'" + currentSpace.getId() + "'";
  }
%>
<script type="text/javascript">
  if (top.window.spAdminWindow) {
    const isRoot = <%=isRoot%>;
    const currentSpaceId = <%=currentSpaceId%>;
    if (isRoot) {
      top.window.spAdminWindow.loadSpace(currentSpaceId);
    } else {
      top.window.spAdminWindow.loadSubSpace(currentSpaceId);
    }
  }
</script>
<% } %>