INSERT INTO uniqueid (maxid, tablename)
VALUES (3, 'st_user'),
       (3, 'domainsp_user');

INSERT INTO st_componentinstance
(id, spaceid, name, componentname, description, createdby, ordernum, createtime, updatetime,
 removetime, componentstatus, updatedby, removedby, ispublic, ishidden, lang, isinheritanceblocked)
VALUES (1, 0, 'Documentation', 'toto', '', 0, 1,
        '1433237280246', '1443424995948', null, null, 1, null, 1, 0, 'fr', 0),
       (2, 0, 'Blog', 'blog', '', 0, 1,
        '1433237280246', '1443424995948', null, null, 1, null, 1, 0, 'fr', 0);

INSERT INTO ST_UserRole(id, instanceId, rolename, name, description, isInherited)
VALUES (1, 1, 'supervisor', '', '', 1),
       (2, 2, 'supervisor', '', '', 1);

INSERT INTO ST_UserRole_User_Rel(userroleid, userId)
VALUES (1, 0),
       (2, 0);

INSERT INTO DomainSP_User (id, firstName, lastName, login, password)
VALUES (1, 'John', 'Anderton', 'john', '$6$MmC1LOXhFD19S$ilZaW83RoStfehIgNRLuAFETXiHYs5/Y8qCmpvFp8xkLRkikXAA10WfaWn.2h9IsQHcKrLYdRu/4UDlbaROBp1'),
       (2, 'Gustave', 'Eiffel', 'gustave', '$6$MmC1LOXhFD19S$ilZaW83RoStfehIgNRLuAFETXiHYs5/Y8qCmpvFp8xkLRkikXAA10WfaWn.2h9IsQHcKrLYdRu/4UDlbaROBp1'),
       (3, 'Bart', 'Simpson', 'bart', '$6$MmC1LOXhFD19S$ilZaW83RoStfehIgNRLuAFETXiHYs5/Y8qCmpvFp8xkLRkikXAA10WfaWn.2h9IsQHcKrLYdRu/4UDlbaROBp1');

INSERT INTO ST_User
(id, domainId, specificId, firstName, lastName, login, accessLevel, state, stateSaveDate,
 notifManualReceiverLimit)
VALUES (1, 0, '1', 'John', 'Anderton', 'john', 'U', 'VALID', '2012-01-01 00:00:00.000', 0),
       (2, 0, '2', 'Gustave', 'Eiffel', 'gustave', 'U', 'VALID', '2012-01-01 00:00:00.000', 0),
       (3, 0, '3', 'Bart', 'Simpson', 'bart', 'U', 'VALID', '2012-01-01 00:00:00.000', 0);

INSERT INTO SB_Node_Node
(nodeid, nodename, nodedescription, nodecreationdate, nodecreatorid, nodepath, nodelevelnumber,
 nodefatherid, modelid, nodestatus, instanceid, type, ordernumber, lang, rightsdependson)
VALUES (0, 'Accueil', 'La Racine', '2020/09/14', '0', '/', 1, -1, NULL, 'Visible', 'toto1',
        'default', 1, 'fr', -1),
       (4, 'Informatique', '', '2020/10/12', 0, '/0/', 2, 0, NULL, 'Invisible', 'toto1',
        'default', 3, 'fr', -1),
       (5, 'Magazines', '', '2020/10/12', 0, '/0/4/', 3, 4, NULL, 'Invisible', 'toto1', 'default',
        1, 'fr', -1);

INSERT INTO SB_Publication_Publi
(pubid, infoid, pubname, pubdescription, pubcreationdate, pubbegindate, pubenddate, pubcreatorid,
 pubimportance, pubversion, pubkeywords, pubcontent, pubstatus, pubupdatedate, instanceid,
 pubupdaterid,
 pubvalidatedate, pubvalidatorid, pubbeginhour, pubendhour, pubauthor, pubtargetvalidatorid,
 pubcloneid,
 pubclonestatus, lang, pubdraftoutdate)
VALUES (1, '0', 'Java Magazine MarchApril 2019', '', '2020/10/23', '2020/10/23', '9999/99/99', '2',
        1, null, '', '', 'Valid', '2020/10/23', 'toto1', '2', NULL, NULL, '00:00', '23:59', NULL,
        NULL,
        -1, NULL, 'en', NULL),
       (4, '0', 'Smalltalk Forever', 'All about this powerful language', '2020/10/23', '2020/10/23',
        '9999/99/99', '2',
        1, null, '', '', 'Valid', '2020/11/27', 'toto1', '2', null, null, '00:00', '23:59', null,
        null,
        -1, null, 'en', null);

INSERT INTO SB_Publication_PubliFather
(pubid, nodeid, instanceid, aliasuserid, aliasdate, puborder)
VALUES (1, 5, 'toto1', NULL, NULL, 1),
       (4, 4, 'toto1', NULL, NULL, 0);

INSERT INTO SB_Thumbnail_Thumbnail
(instanceid, objectid, objecttype, originalattachmentname, modifiedattachmentname, mimetype, xstart,
 ystart, xlength, ylength)
VALUES ('toto1', 4, 1, '1603468931595.png', NULL, 'image/png', NULL, NULL, NULL, NULL);

INSERT INTO Personalization
(id, languages, zoneId, look, personalwspace, thesaurusstatus)
VALUES
    ('1', 'fr', 'Europe/Paris', 'Initial', '', 0),
    ('2', 'fr', 'Europe/Paris', 'Initial', '', 0),
    ('3', 'fr', 'Europe/Paris', 'Initial', '', 0);


-- Some Calendars
INSERT INTO sb_cal_calendar
(id, instanceId, title, zoneid, createDate, createdBy, lastUpdateDate, lastUpdatedBy, version)
VALUES
    ('CAL_ID_1', 'calendar1', 'Calendar 1', 'UTC', '2010-07-28T16:50:00Z', '1', '2010-07-28T16:50:00Z', '1', 0),
    ('CAL_ID_2', 'calendar2', 'Calendar 2', 'UTC', '2010-07-28T16:50:00Z', '2', '2010-07-28T16:55:00Z', '2', 1);

-- Some events
INSERT INTO sb_cal_components
(id, calendarId, startDate, endDate, inDays, title, description, priority, createDate, createdBy, lastUpdateDate, lastUpdatedBy, version)
VALUES
    ('ID_CMP_1', 'CAL_ID_1', '2011-07-08T12:00:00Z', '2011-07-08T13:00:00Z', FALSE, 'RDV1', 'bla blab', 0, '2011-07-01T16:50:00Z', '1', '2011-07-01T16:50:00Z', '1', 0),
    ('ID_CMP_2', 'CAL_ID_2', '2011-07-08T12:00:00Z', '2011-07-08T13:00:00Z', FALSE, 'RDV1', 'bla blab', 0, '2011-07-01T16:50:00Z', '2', '2011-07-01T16:50:00Z', '2', 0),
    ('ID_CMP_3', 'CAL_ID_1', '2011-07-09T13:00:00Z', '2011-07-09T14:00:00Z', FALSE, 'RDV3', 'bla2 blab2', 1, '2011-07-01T16:50:00Z', '1', '2011-07-01T16:50:00Z', '1', 0),
    ('ID_CMP_4', 'CAL_ID_1', '2011-07-09T07:00:00Z', '2011-07-09T08:00:00Z', FALSE, 'RDV4', 'bla4 blab4', 0, '2011-07-01T16:50:00Z', '1', '2011-07-01T16:50:00Z', '1', 0);

INSERT INTO sb_cal_event
(id, componentId, visibility, recurrenceId)
VALUES
    ('ID_E_1', 'ID_CMP_1', 'PRIVATE', NULL),
    ('ID_E_2', 'ID_CMP_2', 'PRIVATE', NULL),
    ('ID_E_3', 'ID_CMP_3', 'PUBLIC', NULL),
    ('ID_E_4', 'ID_CMP_4', 'PUBLIC', NULL);