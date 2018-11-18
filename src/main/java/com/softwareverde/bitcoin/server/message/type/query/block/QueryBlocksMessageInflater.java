package com.softwareverde.bitcoin.server.message.type.query.block;

import com.softwareverde.bitcoin.server.message.BitcoinProtocolMessageInflater;
import com.softwareverde.bitcoin.server.message.header.BitcoinProtocolMessageHeader;
import com.softwareverde.bitcoin.server.message.type.MessageType;
import com.softwareverde.bitcoin.type.hash.sha256.MutableSha256Hash;
import com.softwareverde.bitcoin.type.hash.sha256.Sha256Hash;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.bytearray.Endian;

public class QueryBlocksMessageInflater extends BitcoinProtocolMessageInflater {

    @Override
    public QueryBlocksMessage fromBytes(final byte[] bytes) {
        final Integer blockHeaderHashByteCount = 32;
        final QueryBlocksMessage queryBlocksMessage = new QueryBlocksMessage();
        final ByteArrayReader byteArrayReader = new ByteArrayReader(bytes);

        final BitcoinProtocolMessageHeader protocolMessageHeader = _parseHeader(byteArrayReader, MessageType.QUERY_BLOCKS);
        if (protocolMessageHeader == null) { return null; }

        queryBlocksMessage._version = byteArrayReader.readInteger(4, Endian.LITTLE);

        final Integer blockHeaderCount = byteArrayReader.readVariableSizedInteger().intValue();
        if (blockHeaderCount >= QueryBlocksMessage.MAX_BLOCK_HASH_COUNT) { return null; }

        final Integer bytesRequired = (blockHeaderCount * blockHeaderHashByteCount);
        if (byteArrayReader.remainingByteCount() < bytesRequired) { return null; }

        final MutableList<Sha256Hash> blockHashesInReverseOrder = new MutableList<Sha256Hash>(500);
        for (int i=0; i<blockHeaderCount; ++i) {
            final Sha256Hash blockHash = MutableSha256Hash.wrap(byteArrayReader.readBytes(32, Endian.LITTLE));
            blockHashesInReverseOrder.add(blockHash);
        }
        for (int i=0; i<blockHeaderCount; ++i) {
            final Sha256Hash blockHash = blockHashesInReverseOrder.get(blockHeaderCount - i - 1);
            queryBlocksMessage._blockHeaderHashes.add(blockHash);
        }

        final byte[] blockHeaderHashBytes = byteArrayReader.readBytes(32, Endian.LITTLE);
        queryBlocksMessage._stopBeforeBlockHash = MutableSha256Hash.wrap(blockHeaderHashBytes);

        if (byteArrayReader.didOverflow()) { return null; }

        return queryBlocksMessage;
    }
}
