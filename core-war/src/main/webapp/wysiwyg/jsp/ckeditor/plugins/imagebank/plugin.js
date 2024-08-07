/**
 *    Copyright (C) 2000 - 2024 Silverpeas
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   As a special exception to the terms and conditions of version 3.0 of
 *   the GPL, you may redistribute this Program in connection with Free/Libre
 *   Open Source Software ("FLOSS") applications as described in Silverpeas's
 *   FLOSS exception.  You should have received a copy of the text describing
 *   the FLOSS exception, and it is also available here:
 *   "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Register the plugin within the editor.
CKEDITOR.plugins.add( 'imagebank', {
    // This plugin requires the Widgets System defined in the 'widget' plugin.
	  requires: ['richcombo'],

	  // Register the icon used for the toolbar button. It must be the same
	  // as the name of the widget.
	  icons: 'imagebank',

	  lang: 'de,en,fr',

	  // The plugin initialization logic goes inside this method.
	  init: function( editor ) {
	    var imageBankEnabled = true;
	    if (typeof editor.config.imagebank !== "undefined") {
        imageBankEnabled = editor.config.imagebank;
      }
	    if (imageBankEnabled) {
        var apps;
        var url = webContext + "/services/components?filter=imagebanks";
        $.ajax({
          url : url,
          cache: false,
          async : false
        }).done(function(result) {
          apps = result;
        });

        if (apps && apps.length > 0) {
          if (apps.length === 1) {
            // only once app, adding a button
            editor.addCommand('openImageBank', {
              exec : function(editor) {
                openGalleryFileManager(editor.name, apps[0].name + apps[0].id);
              }
            });

            editor.ui.addButton('imagebank', {
              label : editor.lang.imagebank.onceLabel, command : 'openImageBank'
            });
          } else {
            // several apps, adding a list
            editor.ui.addRichCombo('imagebank', {
              label : editor.lang.imagebank.label,
              title : editor.lang.imagebank.title,
              multiSelect : false,
              className : 'sp-richcombo',
              panel : {
                css : [CKEDITOR.skin.getPath('editor')].concat(editor.config.contentsCss)
              },
              init : function() {
                var self = this;
                apps.forEach(function(app) {
                  self.add(app.name + app.id, app.label, app.label);
                });
              },
              onClick : function(value) {
                openGalleryFileManager(editor.name, value);
              }
            });
          }
        }
      }
    }

});