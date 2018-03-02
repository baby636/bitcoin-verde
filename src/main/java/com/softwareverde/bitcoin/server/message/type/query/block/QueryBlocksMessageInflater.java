package com.softwareverde.bitcoin.server.message.type.query.block;

import com.softwareverde.bitcoin.server.message.ProtocolMessage;
import com.softwareverde.bitcoin.server.message.ProtocolMessageInflater;
import com.softwareverde.bitcoin.server.message.header.ProtocolMessageHeader;
import com.softwareverde.bitcoin.type.hash.Hash;
import com.softwareverde.bitcoin.type.hash.MutableHash;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.bitcoin.util.bytearray.Endian;

public class QueryBlocksMessageInflater extends ProtocolMessageInflater {

    @Override
    public QueryBlocksMessage fromBytes(final byte[] bytes) {
        final Integer blockHeaderHashByteCount = 32;
        final QueryBlocksMessage queryBlocksMessage = new QueryBlocksMessage();
        final ByteArrayReader byteArrayReader = new ByteArrayReader(bytes);

        final ProtocolMessageHeader protocolMessageHeader = _parseHeader(byteArrayReader, ProtocolMessage.MessageType.QUERY_BLOCKS);
        if (protocolMessageHeader == null) { return null; }

        queryBlocksMessage._version = byteArrayReader.readInteger(4, Endian.LITTLE);

        final Integer blockHeaderCount = byteArrayReader.readVariableSizedInteger().intValue();
        if (blockHeaderCount >= QueryBlocksMessage.MAX_BLOCK_HEADER_HASH_COUNT) { return null; }

        final Integer bytesRequired = (blockHeaderCount * blockHeaderHashByteCount);
        if (byteArrayReader.remainingByteCount() < bytesRequired) { return null; }

        for (int i=0; i<blockHeaderCount; ++i) {
            final Hash blockHeaderHash = new MutableHash(byteArrayReader.readBytes(32, Endian.LITTLE));
            queryBlocksMessage._blockHeaderHashes.add(blockHeaderHash);
        }

        final byte[] blockHeaderHashBytes = byteArrayReader.readBytes(32, Endian.LITTLE);
        queryBlocksMessage._desiredBlockHeaderHash = new MutableHash(blockHeaderHashBytes);

        if (byteArrayReader.didOverflow()) { return null; }

        return queryBlocksMessage;
    }
}