package kr.syeyoung.dungeonsguide.mod.whosonline.api.messages;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kr.syeyoung.dungeonsguide.mod.whosonline.api.messages.server.*;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;

/**
 * Utility class for parsing messages into response objects (gson bad kekw)
 */
public class MessageParser {

    private MessageParser(){}

    private static final Gson gson = new Gson();

    /**
     * @param s message string received from server
     * @return instance of {@link AbstractMessage}  example: {@link S07Pong}
     */
    public static @Nullable AbstractMessage parse(@NotNull String s) {
        val m = gson.fromJson(s, ServerMessage.class);
        switch (m.t) {
            case "/connected":
                return new S01ConnectAck(m.c.getAsBoolean());

            case "/is_online":
                val isOnline = m.c.getAsJsonObject().get("is_online").getAsBoolean();
                val uuid = m.c.getAsJsonObject().get("uuid").getAsString();
                val nonce = m.c.getAsJsonObject().get("nonce").getAsString();

                return new S03IsOnlineAck(isOnline, uuid, nonce);

            case "/is_online/bulk":

                val users = new HashMap<String, Boolean>();
                for (val user : m.c.getAsJsonObject().get("users").getAsJsonObject().entrySet()) {
                    users.put(user.getKey(), user.getValue().getAsBoolean());
                }

                val nonce1 = m.c.getAsJsonObject().get("nonce").getAsString();
                return new S05areOnlineAck(Collections.unmodifiableMap(users), nonce1);

            case "/broadcast":
                return new S08Broadcast(m.c.getAsString());

            case "/pong":
                return new S07Pong(m.c.getAsLong());

            default:
                return null;

        }
    }

    /**
     * helper data class that helps us remove a lot of .getAsJsonObject .getMember ceremony
     */
    static class ServerMessage {
        public String t;
        public JsonElement c;
    }


}
