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
<%@ taglib uri="http://www.silverpeas.com/tld/viewGenerator" prefix="view"%>
<%
response.setHeader("Cache-Control","no-store"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires",-1); //prevents caching at the proxy server
%>
<%@ page import="org.silverpeas.core.contribution.attachment.model.DocumentType"%>
<%@ page import="org.silverpeas.kernel.bundle.LocalizationBundle"%>
<%@ page import="org.silverpeas.kernel.bundle.ResourceLocator"%>
<%@ page import="org.silverpeas.kernel.util.StringUtil"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.GraphicElementFactory"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.browsebars.BrowseBar"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.buttonpanes.ButtonPane"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.buttons.Button"%>
<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.frame.Frame"%>

<%@ page import="org.silverpeas.core.web.util.viewgenerator.html.window.Window"%>
<%@ page import="org.silverpeas.core.contribution.content.wysiwyg.service.WysiwygController"%>
<%@ page import="org.owasp.encoder.Encode" %>

<%
  GraphicElementFactory gef = (GraphicElementFactory) session.getAttribute(GraphicElementFactory.GE_FACTORY_SESSION_ATT);
  WysiwygController scc = (WysiwygController) request.getAttribute("wysiwyg");

String spaceLabel = (String) session.getAttribute("WYSIWYG_SpaceLabel");
String componentId = (String) session.getAttribute("WYSIWYG_ComponentId");
String componentLabel = (String) session.getAttribute("WYSIWYG_ComponentLabel");
String browseInformation = Encode.forHtml((String) session.getAttribute("WYSIWYG_BrowseInfo"));
String objectId = (String) session.getAttribute("WYSIWYG_ObjectId");
String language = (String) session.getAttribute("WYSIWYG_Language");
String path = (String) session.getAttribute("WYSIWYG_Path");
String url = "/wysiwyg/jsp/uploadFile.jsp";

String imagesContext = DocumentType.image.toString();

if (StringUtil.isDefined(request.getParameter("ComponentId"))) {
  //case of utilization by a xml template
  componentId = request.getParameter("ComponentId");
  objectId = request.getParameter("ObjectId");
  String context = request.getParameter("Context");
  url += Encode.forHtml("?ComponentId="+componentId+"&ObjectId="+objectId+"&Context="+context);
}

boolean isWebSiteCase = componentId.startsWith(WysiwygController.WYSIWYG_WEBSITES) && StringUtil.isLong(objectId);

LocalizationBundle message = ResourceLocator.getLocalizationBundle("org.silverpeas.wysiwyg.multilang.wysiwygBundle", language);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<view:looknfeel/>
<style type="text/css">
<!--
.eventCells {  padding-right: 3px; padding-left: 3px; vertical-align: top; background-color: #FFFFFF}
-->
</style>
<script type="text/javascript">
function returnHtmlEditor() {
  window.close();
}
</script>

</head>
<body>
<%
  Window window = gef.getWindow();
  window.setPopup(true);
  BrowseBar browseBar = window.getBrowseBar();
  browseBar.setDomainName(spaceLabel);
  browseBar.setComponentName(componentLabel);
  browseBar.setPath(browseInformation);
  browseBar.setClickable(false);

  out.println(window.printBefore());

	Frame frame=gef.getFrame();
	ButtonPane buttonPane = gef.getButtonPane();
	Button button = gef.getFormButton(message.getString("Close"), "javascript:onClick=returnHtmlEditor()", false);
	buttonPane.addButton(button);
	out.println(frame.printBefore());
	out.flush();
	if (isWebSiteCase) {
		getServletConfig().getServletContext().getRequestDispatcher("/wysiwyg/jsp/uploadWebsiteFile.jsp?Path="+path+"&Language="+language).include(request, response);
	} else {
		getServletConfig().getServletContext().getRequestDispatcher("/attachment/jsp/editAttachedFiles.jsp?Id="+objectId+"&ComponentId="+componentId+"&Context="+imagesContext+"&Url="+url+"&OriginWysiwyg=true&SimpleReload=true").include(request, response);
	}
  out.println(frame.printMiddle());
  out.println(frame.printAfter());
  out.println("<center><br/>"+buttonPane.print()+"</center>");
  out.println(window.printAfter());
%>
</body>
</html>