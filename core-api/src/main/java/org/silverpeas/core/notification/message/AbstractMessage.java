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
package org.silverpeas.core.notification.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.owasp.encoder.Encode;

/**
 * Message to notify the user
 * @author Yohann Chastagnier
 * Date: 07/11/13
 */
public abstract class AbstractMessage implements Message {

  private final String content;
  private long displayLiveTime = 0;

  protected AbstractMessage(String content) {
    this.content = content;
  }

  @Override
  public String getContent() {
    return Encode.forHtml(content);
  }

  @Override
  public long getDisplayLiveTime() {
    return displayLiveTime;
  }

  @Override
  public Message setDisplayLiveTime(final long milliseconds) {
    if (milliseconds > 0) {
      displayLiveTime = milliseconds;
    } else {
      displayLiveTime = 0;
    }
    return this;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Message other = (Message) obj;
    EqualsBuilder matcher = new EqualsBuilder();
    matcher.append(getType(), other.getType());
    matcher.append(getContent(), other.getContent());
    matcher.append(getDisplayLiveTime(), other.getDisplayLiveTime());
    return matcher.isEquals();
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hash = new HashCodeBuilder();
    hash.append(getType());
    hash.append(getContent());
    hash.append(getDisplayLiveTime());
    return hash.toHashCode();
  }
}
