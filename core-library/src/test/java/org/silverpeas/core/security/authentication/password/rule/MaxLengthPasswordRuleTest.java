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
package org.silverpeas.core.security.authentication.password.rule;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.silverpeas.core.security.authentication.password.constant.PasswordRuleType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * User: Yohann Chastagnier
 * Date: 08/01/13
 */
class MaxLengthPasswordRuleTest extends AbstractPasswordRuleTest<MaxLengthPasswordRule> {

  @Test
  void testCommons() {
    MaxLengthPasswordRule rule = newRuleInstanceForTest();
    assertThat(rule.getType(), is(PasswordRuleType.MAX_LENGTH));
    assertThat(rule.random(), is(""));
  }

  @Test
  @Override
  public void testDefinedPropertyValues() {
    setDefinedSettings();
    MaxLengthPasswordRule rule = newRuleInstanceForTest();
    assertThat(rule.getValue(), is(20));
    assertThat(rule.isRequired(), is(true));
    assertThat(rule.isCombined(), is(false));
    assertThat(rule.check("1234567890123456789"), is(true));
    assertThat(rule.check("12345678901234567890"), is(true));
    assertThat(rule.check("123456789012345678901"), is(false));
  }

  @Disabled("Doesn't apply in this test class")
  @Override
  public void testDefinedMoreThanOnePropertyValues() {
  }

  @Disabled("Doesn't apply in this test class")
  @Override
  public void testCombinationDefinedMoreThanOnePropertyValues() {
  }

  @Test
  @Override
  public void testNotDefinedPropertyValues() {
    setNotDefinedSettings();
    MaxLengthPasswordRule rule = newRuleInstanceForTest();
    assertThat(rule.getValue(), is(8));
    assertThat(rule.isRequired(), is(false));
    assertThat(rule.isCombined(), is(false));
    assertThat(rule.check("1234567"), is(true));
    assertThat(rule.check("12345678"), is(true));
    assertThat(rule.check("1234567890"), is(false));
  }

  @Test
  @Override
  public void testBadDefinedPropertyValues() {
    setBadDefinedSettings();
    MaxLengthPasswordRule rule = newRuleInstanceForTest();
    assertThat(rule.getValue(), is(8));
    assertThat(rule.isRequired(), is(false));
    assertThat(rule.isCombined(), is(false));
    assertThat(rule.check("1234567"), is(true));
    assertThat(rule.check("12345678"), is(true));
    assertThat(rule.check("1234567890"), is(false));
  }

  @Test
  @Override
  public void testNotRequiredPropertyValues() {
    setNotRequiredSettings();
    MaxLengthPasswordRule rule = newRuleInstanceForTest();
    assertThat(rule.getValue(), is(20));
    assertThat(rule.isRequired(), is(false));
    assertThat(rule.isCombined(), is(false));
    assertThat(rule.check("1234567890123456789"), is(true));
    assertThat(rule.check("12345678901234567890"), is(true));
    assertThat(rule.check("123456789012345678901"), is(false));
  }
}
