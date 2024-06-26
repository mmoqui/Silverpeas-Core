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
package org.silverpeas.core.contribution.attachment.model;

import org.junit.jupiter.api.Test;
import org.silverpeas.kernel.test.UnitTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author ehugonnet
 */
@UnitTest
class UnlockOptionTest {

  /**
   * Test of isSelected method, of class UnlockOption.
   */
  @Test
  void testIsSelected() {
    assertThat(UnlockOption.FORCE.isSelected(0), is(false));
    assertThat(UnlockOption.PRIVATE_VERSION.isSelected(0), is(false));
    assertThat(UnlockOption.FORCE.isSelected(9), is(true));
    assertThat(UnlockOption.PRIVATE_VERSION.isSelected(9), is(true));
    assertThat(UnlockOption.UPLOAD.isSelected(9), is(false));
  }

  /**
   * Test of addOption method, of class UnlockOption.
   */
  @Test
  void testAddOption() {
    int value = 0;
    value = UnlockOption.FORCE.addOption(value);
    assertThat(value, is(8));
    value = UnlockOption.FORCE.addOption(value);
    assertThat(value, is(8));
    value = UnlockOption.PRIVATE_VERSION.addOption(value);
    assertThat(value, is(9));
  }

  /**
   * Test of addOption method, of class UnlockOption.
   */
  @Test
  void testRemoveOption() {
    int value = 0;
    value = UnlockOption.FORCE.removeOption(value);
    assertThat(value, is(0));
    value = UnlockOption.FORCE.addOption(value);
    value = UnlockOption.PRIVATE_VERSION.addOption(value);
    assertThat(value, is(9));
    value = UnlockOption.FORCE.removeOption(value);
    assertThat(value, is(1));
  }
}
