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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

(function() {

  let __converse;

  /**
   * Setups the vcard management in order to get the display of user avatar and user name.
   * @private
   */
  function __setupVCard(chatOptions) {
    chatOptions.whitelisted_plugins.push('silverpeas-vcard');
    converse.plugins.add('silverpeas-vcard', {
      dependencies: ["converse-vcard"],
      initialize : function() {
        const _converse = this._converse;
        const __superGetFn = _converse.api.vcard.get;
        const __superSetFn = _converse.api.vcard.set;
        Object.assign(_converse.api.vcard, {
          get : function(model, force) {
            let _force = force;
            let _curJid = model;
            if (typeof model === 'object') {
              _curJid = model.attributes.jid;
              if (!model._sp_vcard_init) {
                model._sp_vcard_init = true;
                _force = true;
              }
            }
            return __superGetFn.call(this, model, _force).then(function(vCard) {
              for (let key in vCard) {
                if (typeof vCard[key] === 'undefined') {
                  vCard[key] = '';
                }
              }
              if (_curJid === chatOptions.jid) {
                extendsObject(false, chatOptions.vcard, vCard);
              }
              const contact = _converse.roster && _converse.roster.findWhere({'jid' : _curJid});
              if (contact && contact.attributes) {
                const attrs = contact.attributes;
                const newAttrs = ['image', 'image_type'].filter(function(t) {
                  return !!(attrs[t] && vCard[t] && attrs[t] !== vCard[t]);
                });
                if (newAttrs.length > 0) {
                  const newData = {};
                  newAttrs.forEach(function(t) {
                    newData[t] = vCard[t];
                  });
                  contact.save(newData).then(function() {
                    sp.log.info('model update success', _curJid);
                    return vCard;
                  }, function() {
                    sp.log.info('model update error', _curJid);
                    return vCard;
                  });
                }
              }
              return vCard;
            });
          },
          set : function(jid, data) {
            if (jid === chatOptions.jid) {
              data['fn'] = chatOptions.vcard.fn;
              data['fullname'] = chatOptions.vcard.fn;
              data['nickname'] = chatOptions.vcard.fn;
              if (data['image'] !== chatOptions.vcard['image']) {
                delete data['image_hash'];
              }
            }
            return __superSetFn.call(this, jid, data);
          }
        })
      }
    });
  }

  /**
   * Setups the box resize management according to Silverpeas's constraints.
   * @private
   */
  function __setupResize(chatOptions) {
    if (!spWindow) {
      return;
    }
    chatOptions.whitelisted_plugins.push('silverpeas-resize');
    converse.plugins.add('silverpeas-resize', {
      dependencies: ["converse-dragresize"],
      initialize : function() {
        const _converse = this._converse;
        let __isDragging = false;
        const __startDrag = function() {
          if (!__isDragging) {
            spWindow.startsBoxResize();
            __isDragging = true;
          }
        };
        const __endDrag = function() {
          if (__isDragging) {
            spWindow.endsBoxResize();
            __isDragging = false;
          }
        };
        _converse.api.listen.on('startDiagonalResize', __startDrag);
        _converse.api.listen.on('startHorizontalResize', __startDrag);
        _converse.api.listen.on('startVerticalResize', __startDrag);
        document.addEventListener('mouseup', __endDrag);
      }
    });
  }

  /**
   * Setups rooms addons.
   * Rooms API is used on room creation.
   * For now, it configures automatically the creation of a new groupchat room in order to set
   * automatically the topic (to get a readable title for the room).
   * @private
   */
  function __setupRoomsAddons(chatOptions) {
    chatOptions.whitelisted_plugins.push('silverpeas-rooms');
    converse.plugins.add('silverpeas-rooms', {
      dependencies: ["converse-muc"],
      initialize : function() {
        const _converse = this._converse;
        const __superGetFn = _converse.api.rooms.get;
        Object.assign(_converse.api.rooms, {
          get : function(jid, attrs, create) {
            if (create && attrs && typeof jid === 'string' &&
                StringUtil.isNotDefined(attrs.id) &&
                StringUtil.isDefined(attrs.jid) &&
                StringUtil.isNotDefined(attrs.name)) {
              const roomName = Strophe.unescapeNode(attrs.jid.replace(/@.+$/, ''));
              if (roomName.length !== attrs.jid.length) {
                Object.assign(attrs, {
                  'auto_configure' : true,
                  'roomconfig' : {
                    'roomname' : roomName,
                    'membersonly' : true
                  }
                });
              }
            }
            return __superGetFn.call(this, jid, attrs, create);
          }
        });
      }
    });
  }

  /**
   * Setups chatboxes addons.
   * Chatboxes API is used on room display.
   * For now the notifications are handled in the way that unread message counter for MUC is
   * updated num_unread or num_unread_general instead of only num_unread.
   * @private
   */
  function __setupChatboxesAddons(chatOptions) {
    chatOptions.whitelisted_plugins.push('silverpeas-chatboxes');
    converse.plugins.add('silverpeas-chatboxes', {
      dependencies: ["silverpeas-rooms", "converse-chatboxes"],
      initialize : function() {
        const _converse = this._converse;
        if (!!_converse.settings['notify_all_room_messages']) {
          _converse.api.listen.on('chatBoxesInitialized', function() {
            _converse.chatboxes.on('change:num_unread_general', function(chatbox) {
              // num_unread_general means both the direct messages and the messages sent to other
              // participants
              chatbox.save({
                'num_unread' : chatbox.get('num_unread_general')
              });
            });
          });
        }
      }
    });
  }

  /**
   * Setups notification addons.
   * According to OS and WEB browser, notifications could not be handled as attempted.
   * This addon try to improve this part.
   * @private
   */
  function __setupNotificationAddons(chatOptions) {
    if (!spLayout) {
      return;
    }
    chatOptions.whitelisted_plugins.push('silverpeas-notification');
    converse.plugins.add('silverpeas-notification', {
      initialize : function() {
        let __timeout;
        let __lastBlurOrFocus;
        const _converse = this._converse;
        const _blurFocusEventHandler = function(e) {
          if (document.visibilityState !== 'hidden') {
            clearTimeout(__timeout);
            __timeout = setTimeout(function() {
              const ev = {'type' : e.type.endsWith('blur') ? "blur" : "focus"};
              if (__lastBlurOrFocus && ev.type !== __lastBlurOrFocus) {
                sp.log.debug('previous', __lastBlurOrFocus, 'next', ev);
                _converse.saveWindowState(ev);
              }
              __lastBlurOrFocus = ev.type;
            }, 1000);
          }
        }
        const _visibilityEventHandler = function() {
          // If invoked, blur and focus MUST be ignored
          sp.log.debug('visibilitychange event', document.visibilityState);
          clearTimeout(__timeout);
          __lastBlurOrFocus = undefined;
        }
        _converse.api.listen.on('registeredGlobalEventHandlers', function() {
          window.addEventListener("visibilitychange", _visibilityEventHandler);
          window.addEventListener("blur", _blurFocusEventHandler);
          window.addEventListener("focus", _blurFocusEventHandler);
          let __body = spLayout.getBody();
          __body.addEventListener("blur", _blurFocusEventHandler, "noti#ConverseJS");
          __body.addEventListener("focus", _blurFocusEventHandler, "noti#ConverseJS");
        });
      }
    });
  }

  /**
   * Setups the common stuffs in order to get the chat working into Silverpeas.
   * @private
   */
  function __setupSilverpeas(chatOptions) {
    chatOptions.whitelisted_plugins.push('silverpeas-setup');
    converse.plugins.add('silverpeas-setup', {
      dependencies : ["silverpeas-vcard"],
      initialize : function() {
        const _converse = this._converse;
        __converse = _converse;
        const urlAsDataPromise = sp.base64.urlAsData(chatOptions.userAvatarUrl);
        const refreshUserAvatar = function() {
          const promises = [];
          for (let key in _converse.promises) {
            if (key.endsWith('Initialized')) {
              let promise = _converse.promises[key];
              if (sp.promise.isOne(promise)) {
                promises.push(promise);
              }
            }
          }
          return sp.promise.whenAllResolved(promises).then(function() {
            return _converse.api.vcard.get(chatOptions.jid, true).then(function(vCard) {
              urlAsDataPromise.then(function(data) {
                const avatarData = data['justData'];
                if (avatarData !== vCard['image']) {
                  vCard['image'] = avatarData;
                  vCard['image_type'] = data['type'];
                  try {
                    _converse.api.vcard.set(chatOptions.jid, vCard).then(function() {
                      sp.log.info('vcard update success');
                    }, function() {
                      sp.log.error('vcard update error', arguments);
                    });
                  } catch (e) {
                    sp.log.error('vcard update error', e);
                  }
                } else {
                  sp.log.info('vcard is up to date');
                }
              });
            });
          });
        };
        _converse.api.listen.on('connected', function() {
          _converse.promises.roomsPanelRendered.then(function() {
            if (!chatOptions.acl.groupchat.creation) {
              sp.element.querySelectorAll('#conversejs .controlbox-padded a').forEach(function($el, index) {
                if (index !== 0) {
                  $el.remove();
                }
              });
            }
            refreshUserAvatar().then(function() {
              return _converse.api.controlbox.open();
            });
          });
        });
      }
    });
  }

  /**
   * Setups the chat cache storage management.
   * @private
   */
  function __setupCacheStorageManagement(chatOptions) {
    chatOptions.whitelisted_plugins.push('silverpeas-cache-storage-management');
    converse.plugins.add('silverpeas-cache-storage-management', {
      dependencies : [],
      initialize : function() {
        const _converse = this._converse;
        const clean = function(room, onClose) {
          if (room.messages.size() > chatOptions.nbMsgMaxCachedPerRoom) {
            sp.log.debug('message clear performed on', room.get('jid'), 'more than', chatOptions.nbMsgMaxCachedPerRoom, 'messages');
            room.clearMessages().then(function() {
              if (!onClose) {
                return room.fetchMessages();
              }
            });
          } else if (!onClose && room.messages.size() === 0) {
            sp.log.debug('fetching archived message on', room.get('jid'));
            return room.fetchArchivedMessages({'before': ''});
          }
        };
        _converse.api.listen.on('afterMessagesFetched', function(room) {
          return clean(room, false);
        });
        _converse.api.listen.on('chatBoxClosed', function (room) {
          if (room && room.model) {
            const roomType = room.model.get('type');
            if (roomType === _converse.CHATROOMS_TYPE || roomType === _converse.PRIVATE_CHAT_TYPE) {
              clean(room.model, true);
            }
          }
        });
      }
    });
  }

  window.SilverChat = new function() {
    let __settings = {
      'whitelisted_plugins' : []
    };
    this.init = function(chatOptions) {
      __settings = extendsObject(__settings, chatOptions);
      if (spWindow) {
        __settings.whitelisted_plugins.push('silverpeas-sp-permalink');
      }
      if (__settings.visioEnabled) {
        __settings.whitelisted_plugins.push('jitsimeet');
      }
      if (__settings.screencastEnabled) {
        __settings.whitelisted_plugins.push('screencast');
      }
      __setupVCard(__settings);
      __setupResize(__settings);
      __setupRoomsAddons(__settings);
      __setupSilverpeas(__settings);
      __setupChatboxesAddons(__settings);
      __setupNotificationAddons(__settings);
      __setupCacheStorageManagement(__settings);
      return this;
    };
    this.start = function() {
      sp.i18n.load({
        bundle : 'org.silverpeas.chat.multilang.chat',
        async : true
      }).then(function() {
        converse.initialize({
          'view_mode' : __settings.viewMode,
          'loglevel' : __settings.debug ? 'debug' : 'error',
          'i18n' : __settings.language,
          'assets_path' : '/silverpeas/chat/converse/',
          'sounds_path' : '/silverpeas/chat/converse/',
          'play_sounds' : false,
          'bosh_service_url' : __settings.url,
          'allow_logout' : false,
          'auto_login' : true,
          'auto_reconnect' : true,
          'jid' : __settings.jid,
          'default_domain' : __settings.domain,
          'domain_placeholder' : __settings.domain,
          'password' : __settings.password,
          'autocomplete_add_contact' : false,
          'notification_icon' : __settings.notificationLogo,
          'muc_domain' : 'conference.' + __settings.domain,
          'locked_muc_domain' : 'hidden',
          'muc_disable_slash_commands' : true,
          'locked_muc_nickname' : true,
          'nickname' : __settings.vcard.fn,
          'auto_register_muc_nickname' : true,
          'notify_all_room_messages' : true,
          'auto_join_on_invite' : false,
          'roster_groups' : false,
          'allow_adhoc_commands' : false,
          'allow_contact_removal' : false,
          'allow_contact_requests' : false,
          'allow_registration' : false,
          'show_controlbox_by_default' : false,
          'discover_connection_methods' : false,
          'jitsimeet_start_option' : "into_new_tab",
          'whitelisted_plugins' : __settings.whitelisted_plugins
        });
      });
    };
    this.stop = function() {
      __converse.off();
      return __converse.api.user.logout();
    };
  }
})();