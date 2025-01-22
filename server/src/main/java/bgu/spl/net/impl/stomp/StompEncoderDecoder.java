package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StompEncoderDecoder implements MessageEncoderDecoder<StompFrame> {

    private StringBuilder buffer = new StringBuilder(); // Accumulate bytes into a string

    @Override
    public StompFrame decodeNextByte(byte nextByte) {
        if (nextByte == '\u0000') { // Null character indicates end of frame
            StompFrame frame = decode(buffer.toString());
            buffer.setLength(0); // Clear buffer for the next message
            return frame;
        } else {
            buffer.append((char) nextByte);
            return null; // Frame not yet complete
        }
    }

    @Override
    public byte[] encode(StompFrame message) {
        String encodedMessage = encodeFrame(message);
        return encodedMessage.getBytes(StandardCharsets.UTF_8);
    }

    private StompFrame decode(String rawFrame) {
        String[] lines = rawFrame.split("\n");
        if (lines.length == 0) return null;

        String command = lines[0]; // The first line is the command
        Map<String, String> headers = new HashMap<>();
        StringBuilder body = new StringBuilder();

        int i = 1; // Start after the command
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break; // End of headers
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }

        // Read the body (if any)
        for (i = i + 1; i < lines.length; i++) {
            body.append(lines[i]).append("\n");
        }

        // Remove the trailing null character (^@ or '\u0000')
        if (body.length() > 0 && body.charAt(body.length() - 1) == '\u0000') {
            body.setLength(body.length() - 1);
        }

        return new StompFrame(command, headers, body.toString());
    }

    private String encodeFrame(StompFrame frame) {
        StringBuilder encodedFrame = new StringBuilder();
        encodedFrame.append(frame.getCommand()).append("\n");

        for (Map.Entry<String, String> entry : frame.getHeaders().entrySet()) {
            encodedFrame.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }

        encodedFrame.append("\n").append(frame.getBody()).append('\u0000');
        return encodedFrame.toString();
    }
}

