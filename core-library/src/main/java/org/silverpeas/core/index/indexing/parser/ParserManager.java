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
package org.silverpeas.core.index.indexing.parser;

import org.silverpeas.core.annotation.Bean;
import org.silverpeas.kernel.SilverpeasRuntimeException;
import org.silverpeas.kernel.annotation.Technical;
import org.silverpeas.kernel.bundle.ResourceLocator;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.kernel.bundle.SettingBundle;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.silverpeas.core.index.indexing.IndexingLogger.indexingLogger;

/**
 * The ParserManager class manages all the parsers which will be used to parse the indexed files.
 */
@Technical
@Bean
@Singleton
public final class ParserManager {

  /**
   * The map giving the parser for a specific file format. The type of this map is : Map (String ->
   * Parser).
   */
  private final Map<String, Parser> parserMap = new HashMap<>();
  @Inject
  @DefaultParser
  private Parser defaultParser;

  private ParserManager() {
  }

  /**
   * Set all the parsers declared in Parsers.properties file.
   */
  @PostConstruct
  private void init() {
    try {
      SettingBundle parsersConfiguration =
          ResourceLocator.getSettingBundle("org.silverpeas.index.indexing.Parser");
      Set<String> mimeTypes = parsersConfiguration.keySet();
      for (String mimeType : mimeTypes) {
        String parserName = parsersConfiguration.getString(mimeType);
        if ("ignored".equals(parserName) || parserName.isEmpty()) {
          continue; // we skip ignored mime type
        }
        registerParser(parserName, mimeType);
      }
    } catch (MissingResourceException e) {
      indexingLogger().error(e.getMessage(), e);
    }
  }

  private void registerParser(String parserName, String mimeType) {
    try {
      Parser parser = ServiceProvider.getService(parserName);
      parserMap.put(mimeType, parser);
    } catch (SilverpeasRuntimeException e) {
      indexingLogger().error("No parser found in silverpeas for {0}: {1}", mimeType, parserName);
    } catch (Exception e) {
      indexingLogger().error(e.getMessage(), e);
    }
  }

  /**
   * Get the parser for the given file mimetype.
   * @param mimeType a known and supported mimetype.
   * @return an optional {@link Parser} instance.
   */
  public Optional<Parser> getParser(String mimeType) {
    return ofNullable(parserMap.getOrDefault(mimeType, defaultParser));
  }

}
