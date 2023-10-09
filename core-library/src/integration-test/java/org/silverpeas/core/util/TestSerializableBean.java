/*
 * Copyright (C) 2000 - 2022 Silverpeas
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
package org.silverpeas.core.util;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.Serializable;
import java.util.Date;

import static jakarta.json.bind.annotation.JsonbDateFormat.TIME_IN_MILLIS;

/**
 * @author mmoquillon
 */
@SuppressWarnings("unused")
public class TestSerializableBean implements Serializable {

  private static final long serialVersionUID = 8497281393162228469L;

  @JsonbProperty("id")
  private String _id;

  @JsonbProperty("name")
  private String _name;
  @JsonbProperty("date")
  private Date _date;

  protected TestSerializableBean() {

  }

  public TestSerializableBean(final String id, final String name, final Date date) {
    this._id = id;
    this._name = name;
    this._date = date;
  }

  public String getId() {
    return _id;
  }

  public String getName() {
    return _name;
  }

  @JsonbDateFormat(TIME_IN_MILLIS)
  public Date getDate() {
    return _date;
  }

  public void setId(String id) {
    this._id = id;
  }

  public void setName(String name) {
    this._name = name;
  }

  @JsonbDateFormat(TIME_IN_MILLIS)
  public void setDate(Date date) {
    this._date = date;
  }
}
