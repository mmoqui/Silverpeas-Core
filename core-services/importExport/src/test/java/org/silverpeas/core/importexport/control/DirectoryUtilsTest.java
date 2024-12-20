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
package org.silverpeas.core.importexport.control;

import org.junit.jupiter.api.Test;
import org.silverpeas.kernel.test.UnitTest;

import static java.io.File.separatorChar;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author ehugonnet
 */
@UnitTest
public class DirectoryUtilsTest {

  public DirectoryUtilsTest() {
  }

  /**
   * Test of formatToDirectoryNamingCompliant method, of class DirectoryUtils.
   */
  @Test
  public void testFormatToDirectoryNamingCompliant() {
       String fileName = "bart" + separatorChar + "simpson" + separatorChar + "well.txt";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName), is("bart_simpson_well.txt"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "well well.txt";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName), is("bart_simpson_well well.txt"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName), is("bart_simpson_"));

    fileName = "bart" + separatorChar + "simpson" + separatorChar + "prn...txtT";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName),
            is("bart_simpson_prn.txtT"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "test.T*T";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName),
            is("bart_simpson_test.T_T"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "test|.TXT";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName),
            is("bart_simpson_test_.TXT"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "te?st.TXT";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName),
            is("bart_simpson_te_st.TXT"));
    fileName = "bart" + separatorChar + "simpson" + separatorChar + "prn..TXT..";
    assertThat(DirectoryUtils.formatToDirectoryNamingCompliant(fileName),
            is("bart_simpson_prn.TXT."));
  }

  /**
   * Test of removeDots method, of class DirectoryUtils.
   */
  @Test
  public void testRemoveDots() {
    String fileName = "well.txt";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "well well.txt";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));

    fileName = "prn...txtT";
    assertThat(DirectoryUtils.removeDots(fileName), is("prn.txtT"));
    fileName = "test.T*T";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "test|.TXT";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "te?st.TXT";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "con.TXT";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));
    fileName = "prn.TXT";
    assertThat(DirectoryUtils.removeDots(fileName), is(fileName));

    fileName = "prn..TXT..";
    assertThat(DirectoryUtils.removeDots(fileName), is("prn.TXT."));
  }
}
