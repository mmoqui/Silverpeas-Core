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
package org.silverpeas.core.notification.user.client;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.stubbing.Answer;
import org.silverpeas.core.admin.service.OrganizationController;
import org.silverpeas.core.admin.user.model.GroupDetail;
import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.admin.user.service.GroupProvider;
import org.silverpeas.core.admin.user.service.UserProvider;
import org.silverpeas.core.test.unit.extention.JEETestContext;
import org.silverpeas.kernel.test.extension.EnableSilverTestEnv;
import org.silverpeas.kernel.test.annotations.TestManagedMock;
import org.silverpeas.kernel.bundle.SettingBundle;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@EnableSilverTestEnv(context = JEETestContext.class)
class NotificationMetaDataTest {

  private static final String USER_SENDER = "2406";

  @RegisterExtension
  public NotificationSettingsMocker mocker = new NotificationSettingsMocker();

  @TestManagedMock
  private NotificationManager notificationManager;
  private NotificationMetaData current;
  private SettingBundle mockedSettings;

  @BeforeEach
  public void setup(@TestManagedMock OrganizationController controller) throws Exception {
    current = new NotificationMetaData();

    // Settings
    mockedSettings = mocker.getMockedSettings();

    when(UserProvider.get().getUser(anyString())).thenAnswer(
        invocation -> new UserDetail());
    when(controller.getUserDetail(anyString())).thenAnswer(
        invocation -> new UserDetail());
    when(GroupProvider.get().getGroup(anyString())).thenAnswer(invocation -> {
      GroupDetail group = new GroupDetail();
      group.setId((String) invocation.getArguments()[0]);
      group.setUserIds(new String[]{"1", "2", "3", "4", "5"});
      return group;
    });
    when(controller.getGroup(anyString())).thenAnswer(invocation -> {
      GroupDetail group = new GroupDetail();
      group.setId((String) invocation.getArguments()[0]);
      group.setUserIds(new String[] {"1", "2", "3", "4", "5"});
      return group;
    });

    // Notification Manager
    when(notificationManager.getUsersFromGroup(anyString()))
        .thenAnswer((Answer<Collection<UserRecipient>>) invocation -> {
          String groupId = (String) invocation.getArguments()[0];
          return Arrays
              .asList(new UserRecipient(groupId + "_1"), new UserRecipient(groupId + "_2"),
                  new UserRecipient(groupId + "_3"));
        });
  }

  /*
  TESTS around
  {@link NotificationMetaData#displayGroup(String)}.
   */

  @Test
  void displayGroupWithDisplayGroupNotEnabledAndThresholdNotSet() {
    assertThat(current.displayGroup("1"), is(false));
  }

  @Test
  void displayGroupWithDisplayGroupEnabledAndThresholdNotSet() {
    enableDisplayGroup();
    assertThat(current.displayGroup("1"), is(true));
  }

  @Test
  void displayGroupWithDisplayGroupNotEnabledAndHighThresholdSet() {
    setThresholdTo(50);
    assertThat(current.displayGroup("1"), is(false));
  }

  @Test
  void displayGroupWithDisplayGroupNotEnabledAndNbUserInGroupEqualsThresholdSet() {
    setThresholdTo(5);
    assertThat(current.displayGroup("1"), is(false));
  }

  @Test
  void displayGroupWithDisplayGroupNotEnabledAndNbUserInGroupGreaterThanThresholdSet() {
    setThresholdTo(4);
    assertThat(current.displayGroup("1"), is(true));
  }

  /*
  TESTS around
  {@link NotificationMetaData#getAllUserRecipients()}.
   */

  @Test
  void getAllUserRecipientsButNoSenderAndNoRecipient() throws Exception {
    assertThat(current.getAllUserRecipients(), empty());
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsWithUserSenderAndNoRecipient() throws Exception {
    current.setSender(USER_SENDER);

    assertThat(current.getAllUserRecipients(), empty());
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsButNoSenderAndTwoUserRecipients() throws Exception {
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));

    assertThat(current.getAllUserRecipients(),
        containsInAnyOrder(current.getUserRecipients().toArray()));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsWithUserSenderAndTwoUserRecipients() throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));

    assertThat(current.getAllUserRecipients(),
        containsInAnyOrder(current.getUserRecipients().toArray()));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsButNoSenderAndTwoGroupRecipients() throws Exception {
    current.addGroupRecipient(new GroupRecipient("26"));
    current.addGroupRecipient(new GroupRecipient("38"));

    assertThat(current.getAllUserRecipients(),
        Matchers.containsInAnyOrder(new UserRecipient("26_1"), new UserRecipient("26_2"),
            new UserRecipient("26_3"), new UserRecipient("38_1"), new UserRecipient("38_2"),
            new UserRecipient("38_3")));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsWithUserSenderAndTwoGroupRecipients() throws Exception {
    current.setSender(USER_SENDER);
    current.addGroupRecipient(new GroupRecipient("26"));
    current.addGroupRecipient(new GroupRecipient("38"));

    assertThat(current.getAllUserRecipients(),
        Matchers.containsInAnyOrder(new UserRecipient("26_1"), new UserRecipient("26_2"),
            new UserRecipient("26_3"), new UserRecipient("38_1"), new UserRecipient("38_2"),
            new UserRecipient("38_3")));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsButNoSenderAndThreeUserRecipientsAndOneGroupRecipients()
      throws Exception {
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));
    current.addGroupRecipient(new GroupRecipient("GU"));

    assertThat(current.getAllUserRecipients(),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26"),
            new UserRecipient("38"), new UserRecipient("GU_1"), new UserRecipient("GU_2"),
            new UserRecipient("GU_3")));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsWithUserSenderAndThreeUserRecipientsAndOneGroupRecipient()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));
    current.addGroupRecipient(new GroupRecipient("GU"));

    assertThat(current.getAllUserRecipients(),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26"),
            new UserRecipient("38"), new UserRecipient("GU_1"), new UserRecipient("GU_2"),
            new UserRecipient("GU_3")));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void
  getAllUserRecipientsWithUserSenderAndThreeUserRecipientsAndOneGroupRecipientAndModifyingInternalContainer()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));
    current.addGroupRecipient(new GroupRecipient("GU"));

    assertThat(current.getAllUserRecipients(true),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26"),
            new UserRecipient("38"), new UserRecipient("GU_1"), new UserRecipient("GU_2"),
            new UserRecipient("GU_3")));
    assertThat(current.getUserRecipientsToExclude(), empty());
  }

  @Test
  void getAllUserRecipientsWithUserSenderAndThreeUserRecipientsAndOneUserRecipientExcluded()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipientToExclude(new UserRecipient("Excluded"));
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));
    current.addGroupRecipient(new GroupRecipient("GU"));

    assertThat(current.getAllUserRecipients(),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26"),
            new UserRecipient("38"), new UserRecipient("GU_1"), new UserRecipient("GU_2"),
            new UserRecipient("GU_3")));
    assertThat(current.getUserRecipientsToExclude(), contains(new UserRecipient("Excluded")));
  }

  @Test
  void
  getAllUserRecipientsWithUserSenderAndThreeUserRecipientsAndOneUserRecipientExcludedAndModifyingInternalContainer()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipientToExclude(new UserRecipient("Excluded"));
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.addUserRecipient(new UserRecipient("38"));
    current.addGroupRecipient(new GroupRecipient("GU"));

    assertThat(current.getAllUserRecipients(true),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26"),
            new UserRecipient("38"), new UserRecipient("GU_1"), new UserRecipient("GU_2"),
            new UserRecipient("GU_3")));
    assertThat(current.getUserRecipientsToExclude(),
        containsInAnyOrder(new UserRecipient("Excluded")));
  }

  @Test
  void getAllUserRecipientsInSendContextForVerifyingSenderExclusion()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipientToExclude(new UserRecipient("Excluded"));
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));

    assertThat(current.getAllUserRecipients(true),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26")));
    assertThat(current.getUserRecipientsToExclude(),
        containsInAnyOrder(new UserRecipient("Excluded")));
  }

  @Test
  void getAllUserRecipientsInSendContextForVerifyingSenderExclusionWithManualNotification()
      throws Exception {
    current.setSender(USER_SENDER);
    current.addUserRecipientToExclude(new UserRecipient("Excluded"));
    current.addUserRecipient(new UserRecipient(USER_SENDER));
    current.addUserRecipient(new UserRecipient("26"));
    current.manualUserNotification();

    assertThat(current.getAllUserRecipients(true),
        Matchers.containsInAnyOrder(new UserRecipient(USER_SENDER), new UserRecipient("26")));
    assertThat(current.getUserRecipientsToExclude(),
        containsInAnyOrder(new UserRecipient("Excluded")));
  }

  /*
  TESTS around
  {@link NotificationMetaData#isManualUserOne()}.
   */

  @Test
  void isManualUserOneWithNoSenderAndNotManualOne() {
    assertThat(current.isManualUserOne(), is(false));
  }

  @Test
  void isManualUserOneWithSenderAndNotManualOne() {
    current.setSender(USER_SENDER);

    assertThat(current.isManualUserOne(), is(false));
  }

  @Test
  void isManualUserOneWithNoSender() {
    current.manualUserNotification();

    assertThat(current.isManualUserOne(), is(true));
  }

  @Test
  void isManualUserOneWithSender() {
    current.manualUserNotification();
    current.setSender(USER_SENDER);

    assertThat(current.isManualUserOne(), is(true));
  }

  /*
  CURRENT TEST TOOLS
   */

  private void enableDisplayGroup() {
    when(mockedSettings.getBoolean("notif.receiver.displayGroup", false)).thenReturn(true);
  }

  private void setThresholdTo(int value) {
    when(mockedSettings.getInteger("notif.receiver.displayUser.threshold", 0)).thenReturn(value);
  }
}