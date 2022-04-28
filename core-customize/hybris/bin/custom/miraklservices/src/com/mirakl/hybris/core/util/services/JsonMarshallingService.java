package com.mirakl.hybris.core.util.services;

import shaded.com.fasterxml.jackson.core.type.TypeReference;

/**
 * Service handling marshalling and unmarshalling object to and from JSON
 */
public interface JsonMarshallingService {

  /**
   * Transforms JSON to an object of a specific type
   *
   * @param objectToUnmarshal object to unmarshal
   * @param type tagret Class
   * @return unmarshalled object
   */
  <T> T fromJson(String objectToUnmarshal, Class<T> type);

  <T> T fromJson(String objectToUnmarshal, TypeReference<?> typeReference);

  /**
   * Transforms an object of a specific type to JSON
   *
   * @param objectToMarshal object to marshal
   * @param type source Class
   * @return marshalled JSON
   * @deprecated use {@link #toJson(Object, TypeReference)} instead
   */
  @Deprecated
  <T> String toJson(T objectToMarshal, Class<T> type);

  /**
   * Transforms an object of a specific type to JSON
   *
   * @param objectToMarshal object to marshal
   * @return marshalled JSON
   */
  <T> String toJson(T objectToMarshal);

  /**
   * Transforms an object of a specific type to JSON
   *
   * @param objectToMarshal object to marshal
   * @param typeReference the type reference referencing the type of the object to marshal
   * @return marshalled JSON
   */
  <T> String toJson(T objectToMarshal, TypeReference<?> typeReference);
}
