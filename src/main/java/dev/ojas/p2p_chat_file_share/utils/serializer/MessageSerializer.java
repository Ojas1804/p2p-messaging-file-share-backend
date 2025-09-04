package dev.ojas.p2p_chat_file_share.utils.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageSerializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object msg) throws Exception {
        return mapper.writeValueAsString(msg);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }
}
