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

import jakarta.annotation.Nonnull;
import jakarta.json.*;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.silverpeas.core.exception.DecodingException;
import org.silverpeas.core.exception.EncodingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * An encoder of Java bean to a JSON representation and a decoder of JSON stream into the
 * corresponding Java bean. This encoder/decoder goal is to wrap the actual underlying
 * implementation of the JSON parser. So, to ensure a common behaviour among several JSON parsers
 * only the public getters/setters or the public fields are used to access the properties of the
 * bean to serialize/deserialize. Null properties are by default ignored.
 *
 * @author mmoquillon
 */
public class JSONCodec {

  private JSONCodec() {
  }

  /**
   * Encodes the specified bean into a JSON representation.
   *
   * @param bean the bean to encode.
   * @param <T> the type of the bean.
   * @return the JSON representation of the bean in a String.
   * @throws EncodingException if an error occurs while encoding a bean in JSON.
   */
  public static <T> String encode(@Nonnull T bean) {
    requireNonNull(bean, "object");
    try (Jsonb builder = JsonbBuilder.create()) {
      return builder.toJson(bean);
    } catch (Exception e) {
      throw new EncodingException(e.getMessage(), e);
    }
  }

  /**
   * Encodes the bean dynamically built by the specified builder. This method is just a convenient
   * one to dynamically build a JSON representation of a simple bean. We recommend to represent the
   * bean to serialize as a Java object and then to use the method
   * {@code org.silverpeas.core.util.JSONCodec#encode(T bean)}.
   *
   * @param beanBuilder a function that accepts as argument a JSONObject instance and that returns
   * the JSONObject instance enriched with the attributes set by the function.
   * @return the JSON representation of the bean in a String.
   * @throws EncodingException if an error occurs while encoding a bean in JSON.
   */
  public static String encodeObject(@Nonnull UnaryOperator<JSONObject> beanBuilder) {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    JSONObject bean = beanBuilder.apply(new JSONObject(builder));

    try (StringWriter writer = new StringWriter();
         JsonWriter jsonWriter = Json.createWriter(writer)) {
      jsonWriter.writeObject(bean.getJsonObject());
      return writer.toString();
    } catch (IOException ex) {
      throw new EncodingException(ex.getMessage(), ex);
    }
  }

  /**
   * Encodes an array of beans that are dynamically built thank to the specified builder. This
   * method is just a convenient one to dynamically build a JSON representation of an array of
   * simple beans. We recommend to represent the bean to serialize as a Java object and then to use
   * the method {@code org.silverpeas.core.util.JSONCodec#encode(T bean)}.
   *
   * @param arrayBuilder a function that accepts as argument a JSONObject instance and that returns
   * the JSONObject instance enriched with the attributes set by the function.
   * @return the JSON representation of the bean in a String.
   * @throws EncodingException if an error occurs while encoding a bean in JSON.
   */
  public static String encodeArray(@Nonnull UnaryOperator<JSONArray> arrayBuilder) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    JSONArray array = arrayBuilder.apply(new JSONArray(builder));

    try (StringWriter writer = new StringWriter();
         JsonWriter jsonWriter = Json.createWriter(writer)) {
      jsonWriter.writeArray(array.getJsonArray());
      return writer.toString();
    } catch (IOException ex) {
      throw new EncodingException(ex.getMessage(), ex);
    }
  }

  /**
   * Decodes the specified JSON representation into its corresponding bean.
   *
   * @param <T> the type of the bean.
   * @param json the JSON representation of a bean to decode.
   * @param beanType the class of the bean
   * @return the bean decoded from JSON.
   * @throws DecodingException if an error occurs while decoding a JSON String into a bean.
   */
  public static <T> T decode(@Nonnull String json, @Nonnull Class<T> beanType) {
    requireNonNull(json, "JSON data");
    requireNonNull(beanType, "type of the object");
    try (Jsonb builder = JsonbBuilder.create()) {
      return builder.fromJson(json, beanType);
    } catch (Exception e) {
      throw new DecodingException(e.getMessage(), e);
    }
  }

  /**
   * Decodes the specified JSON representation into its corresponding bean.
   *
   * @param <T> the type of the bean.
   * @param jsonStream a stream to a JSON representation of a bean to decode.
   * @param beanType the class of the bean
   * @return the bean decoded from JSON.
   * @throws DecodingException if an error occurs while decoding a JSON stream into a bean.
   */
  public static <T> T decode(InputStream jsonStream, Class<T> beanType) {
    requireNonNull(jsonStream, "input stream");
    requireNonNull(beanType, "type of the object");
    try (Jsonb builder = JsonbBuilder.create()) {
      return builder.fromJson(jsonStream, beanType);
    } catch (Exception e) {
      throw new DecodingException(e.getMessage(), e);
    }
  }

  private static <T> void requireNonNull(final T value, String valueName) {
    if (value == null) {
      throw new IllegalArgumentException("The " + valueName + " must not be null");
    }
  }

  @SuppressWarnings({"UnusedReturnValue", "unused"})
  public static class JSONObject {

    private final JsonObjectBuilder objectNode;

    protected JSONObject(JsonObjectBuilder objectNode) {
      this.objectNode = objectNode;
    }

    protected JsonObject getJsonObject() {
      return this.objectNode.build();
    }

    public JSONObject putNull(final String fieldName) {
      objectNode.add(fieldName, JsonValue.NULL);
      return this;
    }

    public JSONObject put(final String fieldName, final Number v) {
      if (v instanceof Short) {
        put(fieldName, (Short) v);
      } else if (v instanceof Integer) {
        put(fieldName, (Integer) v);
      } else if (v instanceof Long) {
        put(fieldName, (Long) v);
      } else if (v instanceof Float) {
        put(fieldName, (Float) v);
      } else if (v instanceof Double) {
        put(fieldName, (Double) v);
      } else if (v instanceof BigDecimal) {
        put(fieldName, (BigDecimal) v);
      } else if (v != null) {
        put(fieldName, encode(v));
      } else {
        putNull(fieldName);
      }
      return this;
    }

    public JSONObject put(final String fieldName, final Short v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final Integer v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final Long v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final Float v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final Double v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final BigDecimal v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final String v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject put(final String fieldName, final Boolean v) {
      objectNode.add(fieldName, v);
      return this;
    }

    public JSONObject putJSONArray(final String fieldName, UnaryOperator<JSONArray> arrayBuilder) {
      JsonArrayBuilder builder = Json.createArrayBuilder();
      JSONArray array = arrayBuilder.apply(new JSONArray(builder));
      objectNode.add(fieldName, array.getJsonArray());
      return this;
    }

    public JSONObject putJSONObject(final String fieldName,
        UnaryOperator<JSONObject> objectBuilder) {
      JsonObjectBuilder builder = Json.createObjectBuilder();
      JSONObject object = objectBuilder.apply(new JSONObject(builder));
      objectNode.add(fieldName, object.getJsonObject());
      return this;
    }
  }

  @SuppressWarnings({"UnusedReturnValue", "unused"})
  public static class JSONArray {

    private final JsonArrayBuilder arrayNode;

    protected JSONArray(JsonArrayBuilder arrayNode) {
      this.arrayNode = arrayNode;
    }

    protected JsonArray getJsonArray() {
      return this.arrayNode.build();
    }

    public JSONArray add(final Number v) {
      if (v instanceof Short) {
        add((Short) v);
      } else if (v instanceof Integer) {
        add((Integer) v);
      } else if (v instanceof Long) {
        add((Long) v);
      } else if (v instanceof Float) {
        add((Float) v);
      } else if (v instanceof Double) {
        add((Double) v);
      } else if (v instanceof BigDecimal) {
        add((BigDecimal) v);
      } else if (v != null) {
        add(encode(v));
      }
      return this;
    }

    public JSONArray add(final Short v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final Integer v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final Long v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final Float v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final Double v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final BigDecimal v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final String v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray add(final Boolean v) {
      arrayNode.add(v);
      return this;
    }

    public JSONArray addJSONObject(UnaryOperator<JSONObject> beanBuilder) {
      JsonObjectBuilder builder = Json.createObjectBuilder();
      JSONObject object = beanBuilder.apply(new JSONObject(builder));
      arrayNode.add(object.getJsonObject());
      return this;
    }

    public JSONArray addJSONArray(UnaryOperator<JSONArray> arrayBuilder) {
      JsonArrayBuilder builder = Json.createArrayBuilder();
      JSONArray array = arrayBuilder.apply(new JSONArray(builder));
      arrayNode.add(array.getJsonArray());
      return this;
    }

    public JSONArray addJSONArray(List<String> elements) {
      for (String element : elements) {
        arrayNode.add(element);
      }
      return this;
    }
  }
}
