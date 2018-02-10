package com.softwareverde.bitcoin.server.socket.message.ping;

import com.softwareverde.bitcoin.server.socket.message.ProtocolMessage;
import com.softwareverde.bitcoin.server.socket.message.ProtocolMessageHeader;
import com.softwareverde.bitcoin.server.socket.message.ProtocolMessageInflater;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.bitcoin.util.bytearray.Endian;

public class PingMessageInflater extends ProtocolMessageInflater {

    @Override
    public PingMessage fromBytes(final byte[] bytes) {
        final PingMessage pingMessage = new PingMessage();
        final ByteArrayReader byteArrayReader = new ByteArrayReader(bytes);

        final ProtocolMessageHeader protocolMessageHeader = _parseHeader(byteArrayReader, ProtocolMessage.MessageType.PING);
        if (protocolMessageHeader == null) { return null; }

        pingMessage._nonce = byteArrayReader.readLong(8, Endian.LITTLE);
        return pingMessage;
    }
}
