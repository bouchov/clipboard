package com.bouchov.clipboard.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Alexandre Y. Bouchov
 * Date: 16.07.2021
 * Time: 13:31
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class RawJsonDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser jp, DeserializationContext deserializationContext)
            throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);
        return mapper.writeValueAsString(node);
    }
}
