package jp.ac.titech.itpro.sdl.chat.message;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

public class ChatMessageReader implements Closeable {
    private final static String TAG = ChatMessageReader.class.getSimpleName();
    private final JsonReader reader;

    public ChatMessageReader(JsonReader reader) {
        this.reader = reader;
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "close");
        reader.close();
    }

    public boolean hasNext() throws IOException {
        Log.d(TAG, "hasNext");
        return reader.hasNext();
    }

    public void beginArray() throws IOException {
        Log.d(TAG, "beginArray");
        reader.beginArray();
    }

    public void endArray() throws IOException {
        Log.d(TAG, "endArray");
        reader.endArray();
    }

    public ChatMessage read() throws IOException {
        Log.d(TAG, "read");
        int seq = -1;
        long time = -1;
        boolean sound = false;
        String content = null;
        String sender = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
            case ChatMessage.FIELD_SEQ:
                seq = reader.nextInt();
                break;
            case ChatMessage.FIELD_TIME:
                time = reader.nextLong();
                break;
            case ChatMessage.FIELD_CONTENT:
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue();
                    content = null;
                } else {
                    content = reader.nextString();
                }
                break;
            case ChatMessage.FIELD_SENDER:
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue();
                    sender = null;
                } else {
                    sender = reader.nextString();
                }
                break;
            case ChatMessage.FIELD_SOUND:
                sound = (reader.nextInt() == 1);
                break;
            default:
                reader.skipValue();
                break;
            }
        }
        reader.endObject();
        return new ChatMessage(seq, time, content, sender, sound);
    }
}
