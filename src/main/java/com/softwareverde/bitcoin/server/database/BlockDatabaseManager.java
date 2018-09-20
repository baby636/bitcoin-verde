package com.softwareverde.bitcoin.server.database;

import com.softwareverde.bitcoin.block.Block;
import com.softwareverde.bitcoin.block.BlockId;
import com.softwareverde.bitcoin.block.MutableBlock;
import com.softwareverde.bitcoin.block.header.BlockHeader;
import com.softwareverde.bitcoin.block.header.MutableBlockHeader;
import com.softwareverde.bitcoin.block.header.difficulty.Difficulty;
import com.softwareverde.bitcoin.block.header.difficulty.ImmutableDifficulty;
import com.softwareverde.bitcoin.block.header.difficulty.work.BlockWork;
import com.softwareverde.bitcoin.block.header.difficulty.work.ChainWork;
import com.softwareverde.bitcoin.block.header.difficulty.work.MutableChainWork;
import com.softwareverde.bitcoin.chain.segment.BlockChainSegment;
import com.softwareverde.bitcoin.chain.segment.BlockChainSegmentId;
import com.softwareverde.bitcoin.chain.time.MedianBlockTime;
import com.softwareverde.bitcoin.chain.time.MutableMedianBlockTime;
import com.softwareverde.bitcoin.server.database.cache.Cache;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionId;
import com.softwareverde.bitcoin.type.hash.sha256.MutableSha256Hash;
import com.softwareverde.bitcoin.type.hash.sha256.Sha256Hash;
import com.softwareverde.bitcoin.type.merkleroot.MerkleRoot;
import com.softwareverde.bitcoin.type.merkleroot.MutableMerkleRoot;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.immutable.ImmutableListBuilder;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.Query;
import com.softwareverde.database.Row;
import com.softwareverde.database.mysql.MysqlDatabaseConnection;
import com.softwareverde.io.Logger;
import com.softwareverde.util.HexUtil;
import com.softwareverde.util.Util;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class BlockDatabaseManager {
    public static final Cache<BlockId, BlockChainSegmentId> BLOCK_CHAIN_SEGMENT_CACHE = new Cache<BlockId, BlockChainSegmentId>("Block-BlockChainSegmentId", 1460);

    public static final Object MUTEX = new Object();

    /**
     * Initializes a MedianBlockTime from the database.
     *  NOTE: The headBlockHash is included within the MedianBlockTime.
     */
    protected static MutableMedianBlockTime _newInitializedMedianBlockTime(final MysqlDatabaseConnection databaseConnection, final Sha256Hash headBlockHash) throws DatabaseException {
        // Initializes medianBlockTime with the N most recent blocks...

        final MutableMedianBlockTime medianBlockTime = new MutableMedianBlockTime();
        final BlockDatabaseManager blockDatabaseManager = new BlockDatabaseManager(databaseConnection);

        final java.util.List<BlockHeader> blockHeadersInDescendingOrder = new java.util.ArrayList<BlockHeader>(MedianBlockTime.BLOCK_COUNT);

        Sha256Hash blockHash = headBlockHash;
        for (int i = 0; i < MedianBlockTime.BLOCK_COUNT; ++i) {
            final BlockId blockId = blockDatabaseManager.getBlockIdFromHash(blockHash);
            if (blockId == null) { break; }

            final BlockHeader blockHeader = blockDatabaseManager.getBlockHeader(blockId);
            blockHeadersInDescendingOrder.add(blockHeader);
            blockHash = blockHeader.getPreviousBlockHash();
        }

        // Add the blocks to the MedianBlockTime in ascending order (lowest block-height is added first)...
        for (int i = 0; i < blockHeadersInDescendingOrder.size(); ++i) {
            final BlockHeader blockHeader = blockHeadersInDescendingOrder.get(blockHeadersInDescendingOrder.size() - i - 1);
            medianBlockTime.addBlock(blockHeader);
        }

        return medianBlockTime;
    }

    protected final MysqlDatabaseConnection _databaseConnection;

    public BlockDatabaseManager(final MysqlDatabaseConnection databaseConnection) {
        _databaseConnection = databaseConnection;
    }

    protected Long _getBlockHeightForBlockId(final BlockId blockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, block_height FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );

        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return row.getLong("block_height");
    }

    protected BlockId _getBlockIdFromHash(final Sha256Hash blockHash) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM blocks WHERE hash = ?")
                .setParameter(blockHash)
        );

        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return BlockId.wrap(row.getLong("id"));
    }

    protected Sha256Hash _getBlockHashFromId(final BlockId blockId) throws DatabaseException {
        if (blockId == null) { return new MutableSha256Hash(); }

        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, hash FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );

        if (rows.isEmpty()) {
            return new MutableSha256Hash();
        }
        else {
            final Row previousBlockRow = rows.get(0);
            final String hashString = previousBlockRow.getString("hash");
            return Sha256Hash.fromHexString(hashString);
        }
    }

    protected BlockHeader _inflateBlockHeader(final BlockId blockId) throws DatabaseException {
        final Row row;
        {
            final java.util.List<Row> rows = _databaseConnection.query(
                new Query("SELECT * FROM blocks WHERE id = ?")
                    .setParameter(blockId)
            );

            if (rows.isEmpty()) { return null; }
            row = rows.get(0);
        }

        final Sha256Hash previousBlockHash;
        {
            final BlockId previousBlockId = BlockId.wrap(row.getLong("previous_block_id"));
            previousBlockHash = _getBlockHashFromId(previousBlockId);
        }

        final MutableBlockHeader blockHeader = new MutableBlockHeader();
        blockHeader.setPreviousBlockHash(previousBlockHash);
        blockHeader.setVersion(row.getLong("version"));
        blockHeader.setMerkleRoot(MutableMerkleRoot.fromHexString(row.getString("merkle_root")));
        blockHeader.setTimestamp(row.getLong("timestamp"));
        blockHeader.setDifficulty(ImmutableDifficulty.decode(HexUtil.hexStringToByteArray(row.getString("difficulty"))));
        blockHeader.setNonce(row.getLong("nonce"));

        { // Assert that the hashes match after inflation...
            final Sha256Hash expectedHash = Sha256Hash.fromHexString(row.getString("hash"));
            final Sha256Hash actualHash = blockHeader.getHash();
            if (! Util.areEqual(expectedHash, actualHash)) {
                Logger.log("ERROR: Unable to inflate block: " + blockHeader.getHash());
                return null;
            }
        }

        return blockHeader;
    }

    protected void _insertBlockTransactions(final BlockId blockId, final List<Transaction> transactions) throws DatabaseException {
        final TransactionDatabaseManager transactionDatabaseManager = new TransactionDatabaseManager(_databaseConnection);

        for (final Transaction transaction : transactions) {
            final TransactionId transactionId = transactionDatabaseManager.insertTransaction(transaction);
            transactionDatabaseManager.associateTransactionToBlock(transactionId, blockId);
        }
    }

    protected void _updateBlockHeader(final BlockId blockId, final BlockHeader blockHeader) throws DatabaseException {
        final BlockId previousBlockId = _getBlockIdFromHash(blockHeader.getPreviousBlockHash());
        final Long previousBlockHeight = _getBlockHeightForBlockId(previousBlockId);
        final Long blockHeight = (previousBlockHeight == null ? 0 : (previousBlockHeight + 1));

        _databaseConnection.executeSql(
            new Query("UPDATE blocks SET hash = ?, previous_block_id = ?, block_height = ?, merkle_root = ?, version = ?, timestamp = ?, difficulty = ?, nonce = ? WHERE id = ?")
                .setParameter(blockHeader.getHash())
                .setParameter(previousBlockId)
                .setParameter(blockHeight)
                .setParameter(blockHeader.getMerkleRoot())
                .setParameter(blockHeader.getVersion())
                .setParameter(blockHeader.getTimestamp())
                .setParameter(blockHeader.getDifficulty().encode())
                .setParameter(blockHeader.getNonce())
                .setParameter(blockId)
        );
    }

    protected ChainWork _getChainWork(final BlockId blockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, chain_work FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );

        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return ChainWork.fromHexString(row.getString("chain_work"));
    }

    protected BlockId _insertBlockHeader(final BlockHeader blockHeader) throws DatabaseException {
        final BlockId previousBlockId = _getBlockIdFromHash(blockHeader.getPreviousBlockHash());
        final Long previousBlockHeight = _getBlockHeightForBlockId(previousBlockId);
        final Long blockHeight = (previousBlockId == null ? 0 : (previousBlockHeight + 1));
        final Difficulty difficulty = blockHeader.getDifficulty();

        final BlockWork blockWork = difficulty.calculateWork();
        final ChainWork previousChainWork = (previousBlockId == null ? new MutableChainWork() : _getChainWork(previousBlockId));
        final ChainWork chainWork = ChainWork.add(previousChainWork, blockWork);

        return BlockId.wrap(_databaseConnection.executeSql(
            new Query("INSERT INTO blocks (hash, previous_block_id, block_height, merkle_root, version, timestamp, difficulty, nonce, chain_work) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(blockHeader.getHash())
                .setParameter(previousBlockId)
                .setParameter(blockHeight)
                .setParameter(blockHeader.getMerkleRoot())
                .setParameter(blockHeader.getVersion())
                .setParameter(blockHeader.getTimestamp())
                .setParameter(difficulty.encode())
                .setParameter(blockHeader.getNonce())
                .setParameter(chainWork)
        ));
    }

    protected void _setBlockChainSegmentId(final BlockId blockId, final BlockChainSegmentId blockChainSegmentId) throws DatabaseException {
        BLOCK_CHAIN_SEGMENT_CACHE.cacheItem(blockId, blockChainSegmentId);

        _databaseConnection.executeSql(
            new Query("UPDATE blocks SET block_chain_segment_id = ? WHERE id = ?")
                .setParameter(blockChainSegmentId)
                .setParameter(blockId)
        );
    }

    protected BlockChainSegmentId _getBlockChainSegmentId(final BlockId blockId) throws DatabaseException {
        { // Attempt to find BlockChainSegmentId from cache...
            final BlockChainSegmentId cachedBlockChainSegmentId = BLOCK_CHAIN_SEGMENT_CACHE.getCachedItem(blockId);
            if (cachedBlockChainSegmentId != null) {
                return cachedBlockChainSegmentId;
            }
        }

        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, block_chain_segment_id FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );
        if (rows.isEmpty()) { return null; }
        final Row row = rows.get(0);

        final BlockChainSegmentId blockChainSegmentId = BlockChainSegmentId.wrap(row.getLong("block_chain_segment_id"));
        BLOCK_CHAIN_SEGMENT_CACHE.cacheItem(blockId, blockChainSegmentId);
        return blockChainSegmentId;
    }

    protected BlockId _getBlockIdAtBlockHeight(final BlockChainSegmentId blockChainSegmentId, final Long blockHeight) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM blocks WHERE block_chain_segment_id = ? AND block_height = ?")
                .setParameter(blockChainSegmentId)
                .setParameter(blockHeight)
        );

        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return BlockId.wrap(row.getLong("id"));
    }

    protected List<Transaction> _getBlockTransactions(final BlockId blockId) throws DatabaseException {
        final TransactionDatabaseManager transactionDatabaseManager = new TransactionDatabaseManager(_databaseConnection);
        final List<TransactionId> transactionIds = transactionDatabaseManager.getTransactionIds(blockId);

        final ImmutableListBuilder<Transaction> listBuilder = new ImmutableListBuilder<Transaction>(transactionIds.getSize());
        for (final TransactionId transactionId : transactionIds) {
            final Transaction transaction = transactionDatabaseManager.getTransaction(transactionId);
            listBuilder.add(transaction);
        }
        return listBuilder.build();
    }

    protected BlockHeader _blockHeaderFromDatabaseConnection(final BlockId blockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT * FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );

        if (rows.isEmpty()) { return null; }
        final Row row = rows.get(0);

        final Long version = row.getLong("version");

        final Sha256Hash previousBlockHash;
        {
            final BlockId previousBlockId = BlockId.wrap(row.getLong("previous_block_id"));
            previousBlockHash = _getBlockHashFromId(previousBlockId);
        }

        final MerkleRoot merkleRoot = MutableMerkleRoot.fromHexString(row.getString("merkle_root"));
        final Long timestamp = row.getLong("timestamp");
        final Difficulty difficulty = ImmutableDifficulty.decode(HexUtil.hexStringToByteArray(row.getString("difficulty")));
        final Long nonce = row.getLong("nonce");

        final MutableBlockHeader mutableBlockHeader = new MutableBlockHeader();

        mutableBlockHeader.setVersion(version);
        mutableBlockHeader.setPreviousBlockHash(previousBlockHash);
        mutableBlockHeader.setMerkleRoot(merkleRoot);
        mutableBlockHeader.setTimestamp(timestamp);
        mutableBlockHeader.setDifficulty(difficulty);
        mutableBlockHeader.setNonce(nonce);

        return mutableBlockHeader;
    }

    protected BlockId _getChildBlockId(final BlockChainSegmentId blockChainSegmentId, final BlockId previousBlockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM blocks WHERE previous_block_id = ?")
                .setParameter(previousBlockId)
        );

        if (rows.isEmpty()) { return null; }

        if (rows.size() == 1) {
            final Row row = rows.get(0);
            return BlockId.wrap(row.getLong("id"));
        }

        // At this point, previousBlockId has multiple children.
        // If blockChainSegmentId is not provided, then just return the first-seen block.
        if (blockChainSegmentId == null) {
            final Row row = rows.get(0);
            return BlockId.wrap(row.getLong("id"));
        }

        // Since blockChainSegmentId is provided, the child along its chain is the blockId that shall be preferred...
        for (final Row row : rows) {
            final BlockId blockId = BlockId.wrap(row.getLong("id"));
            if (_isBlockConnectedToChain(blockId, blockChainSegmentId, BlockRelationship.ANCESTOR)) {
                return blockId;
            }
        }

        // None of the children blocks match the blockChainSegmentId, so null is returned.
        return null;
    }

    protected Boolean _isBlockConnectedToChain(final BlockId blockId, final BlockChainSegmentId blockChainSegmentId, final BlockRelationship blockRelationship) throws DatabaseException {
        final BlockChainDatabaseManager blockChainDatabaseManager = new BlockChainDatabaseManager(_databaseConnection);

        final Long blockHeight = _getBlockHeightForBlockId(blockId);
        final BlockChainSegmentId blockIdBlockChainSegmentId = _getBlockChainSegmentId(blockId);

        BlockChainSegmentId queriedBlockChainSegmentId = blockChainSegmentId;
        while (true) {
            final BlockChainSegment blockChainSegment = blockChainDatabaseManager.getBlockChainSegment(queriedBlockChainSegmentId);
            if (blockChainSegment == null) { break; }

            final long lowerBoundHeight = (blockChainSegment.getBlockHeight() - blockChainSegment.getBlockCount() + 1);
            final long upperBoundHeight = (blockChainSegment.getBlockHeight());
            if (lowerBoundHeight <= blockHeight && blockHeight <= upperBoundHeight) {
                final BlockId blockIdAtChainSegmentAndHeight = _getBlockIdAtBlockHeight(queriedBlockChainSegmentId, blockHeight);
                return (Util.areEqual(blockId, blockIdAtChainSegmentAndHeight));
            }

            final BlockId nextBlockId;
            {
                if (blockHeight < lowerBoundHeight) {
                    if (blockRelationship == BlockRelationship.DESCENDANT) { return false; }

                    nextBlockId = blockChainSegment.getTailBlockId();
                }
                else {
                    if (blockRelationship == BlockRelationship.ANCESTOR) { return false; }

                    final BlockId headBlockId = blockChainSegment.getHeadBlockId();
                    nextBlockId = _getChildBlockId(blockIdBlockChainSegmentId, headBlockId);
                    if (nextBlockId == null) { break; }
                }
            }

            final BlockChainSegmentId nextBlockChainSegmentId = _getBlockChainSegmentId(nextBlockId);
            if (queriedBlockChainSegmentId.equals(nextBlockChainSegmentId)) { break; }

            queriedBlockChainSegmentId = nextBlockChainSegmentId;
        }

        return false;
    }

    protected BlockChainSegmentId _getParentBlockChainSegmentId(final BlockHeader block) throws DatabaseException {
        final Sha256Hash previousBlockHash = block.getPreviousBlockHash();
        final BlockId previousBlockId = _getBlockIdFromHash(previousBlockHash);
        if (previousBlockId == null) { return null; }

        return _getBlockChainSegmentId(previousBlockId);
    }


    protected BlockId _getHeadBlockId() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT blocks.id, blocks.hash FROM blocks INNER JOIN block_transactions ON block_transactions.block_id = blocks.id ORDER BY blocks.block_height DESC LIMIT 1")
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return BlockId.wrap(row.getLong("id"));
    }


    protected Sha256Hash _getHeadBlockHash() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT blocks.id, blocks.hash FROM blocks INNER JOIN block_transactions ON block_transactions.block_id = blocks.id ORDER BY blocks.block_height DESC LIMIT 1")
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return Sha256Hash.fromHexString(row.getString("hash"));
    }

    protected Sha256Hash _getHeadBlockHeaderHash() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, hash FROM blocks ORDER BY block_height DESC LIMIT 1")
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return Sha256Hash.fromHexString(row.getString("hash"));
    }

    protected BlockId _getHeadBlockHeaderId() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, hash FROM blocks ORDER BY block_height DESC LIMIT 1")
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return BlockId.wrap(row.getLong("id"));
    }

    protected Integer _getBlockDirectDescendantCount(final BlockId blockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM blocks WHERE previous_block_id = ?")
                .setParameter(blockId)
        );

        return (rows.size());
    }

    public BlockId insertBlockHeader(final BlockHeader blockHeader) throws DatabaseException {
        final BlockId blockId = _insertBlockHeader(blockHeader);

        final BlockChainDatabaseManager blockChainDatabaseManager = new BlockChainDatabaseManager(_databaseConnection);
        blockChainDatabaseManager.updateBlockChainsForNewBlock(blockHeader);

        return blockId;
    }

    public void updateBlockHeader(final BlockId blockId, final BlockHeader blockHeader) throws DatabaseException {
        _updateBlockHeader(blockId, blockHeader);
    }

    public BlockId storeBlockHeader(final BlockHeader blockHeader) throws DatabaseException {
        final BlockId existingBlockId = _getBlockIdFromHash(blockHeader.getHash());

        if (existingBlockId != null) {
            // _updateBlockHeader(existingBlockId, blockHeader);
            return existingBlockId;
        }

        final BlockId blockId = _insertBlockHeader(blockHeader);

        final BlockChainDatabaseManager blockChainDatabaseManager = new BlockChainDatabaseManager(_databaseConnection);
        blockChainDatabaseManager.updateBlockChainsForNewBlock(blockHeader);

        return blockId;
    }

    /**
     * Inserts the Block (and BlockHeader if it does not exist) (including its transactions) into the database.
     *  If the BlockHeader has already been stored, this will update the existing BlockHeader.
     *  Transactions inserted on this chain are assumed to be a part of the parent's chain if the BlockHeader did not exist.
     */
    public BlockId storeBlock(final Block block) throws DatabaseException {
        final Sha256Hash blockHash = block.getHash();
        final BlockId existingBlockId = _getBlockIdFromHash(blockHash);

        final BlockId blockId;
        if (existingBlockId == null) {
            blockId = _insertBlockHeader(block);

            final BlockChainDatabaseManager blockChainDatabaseManager = new BlockChainDatabaseManager(_databaseConnection);
            blockChainDatabaseManager.updateBlockChainsForNewBlock(block);
        }
        else {
            blockId = existingBlockId;
        }

        _insertBlockTransactions(blockId, block.getTransactions());

        return blockId;
    }

    /**
     * Inserts the Block (including its transactions) into the database.
     *  If the BlockHeader has already been stored, this function will throw a DatabaseException.
     *  Transactions inserted on this chain are assumed to be a part of the parent's chain.
     */
    public BlockId insertBlock(final Block block) throws DatabaseException {
        final BlockId blockId = _insertBlockHeader(block);

        final BlockChainDatabaseManager blockChainDatabaseManager = new BlockChainDatabaseManager(_databaseConnection);
        blockChainDatabaseManager.updateBlockChainsForNewBlock(block);

        _insertBlockTransactions(blockId, block.getTransactions());
        return blockId;
    }

    public void setBlockByteCount(final BlockId blockId, final Integer byteCount) throws DatabaseException {
        _databaseConnection.executeSql(
            new Query("UPDATE blocks SET byte_count = ? WHERE id = ?")
                .setParameter(byteCount)
                .setParameter(blockId)
        );
    }

    public Integer getBlockByteCount(final BlockId blockId) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, byte_count FROM blocks WHERE id = ?")
                .setParameter(blockId)
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        return row.getInteger("byte_count");
    }

    /**
     * Returns the Sha256Hash of the block that has the tallest block-height.
     */
    public Sha256Hash getHeadBlockHeaderHash() throws DatabaseException {
        return _getHeadBlockHeaderHash();
    }

    /**
     * Returns the BlockId of the block that has the tallest block-height.
     */
    public BlockId getHeadBlockHeaderId() throws DatabaseException {
        return _getHeadBlockHeaderId();
    }

    /**
     * Returns the Sha256Hash of the block that has the tallest block-height that has been fully downloaded (i.e. has transactions).
     */
    public Sha256Hash getHeadBlockHash() throws DatabaseException {
        return _getHeadBlockHash();
    }

    /**
     * Returns the BlockId of the block that has the tallest block-height that has been fully downloaded (i.e. has transactions).
     */
    public BlockId getHeadBlockId() throws DatabaseException {
        return _getHeadBlockId();
    }

    public BlockId getBlockIdFromHash(final Sha256Hash blockHash) throws DatabaseException {
        return _getBlockIdFromHash(blockHash);
    }

    public BlockHeader getBlockHeader(final BlockId blockId) throws DatabaseException {
        return _inflateBlockHeader(blockId);
    }

    public Boolean blockExists(final Sha256Hash blockHash) throws DatabaseException {
        final BlockId blockId = _getBlockIdFromHash(blockHash);
        if (blockId == null) { return false; }

        final TransactionDatabaseManager transactionDatabaseManager = new TransactionDatabaseManager(_databaseConnection);
        final Integer transactionCount = transactionDatabaseManager.getTransactionCount(blockId);
        return (transactionCount > 0);
    }

    public Integer getBlockDirectDescendantCount(final BlockId blockId) throws DatabaseException {
        return _getBlockDirectDescendantCount(blockId);
    }

    public void setBlockChainSegmentId(final BlockId blockId, final BlockChainSegmentId blockChainSegmentId) throws DatabaseException {
        _setBlockChainSegmentId(blockId, blockChainSegmentId);
    }

    public BlockChainSegmentId getBlockChainSegmentId(final BlockId blockId) throws DatabaseException {
        return _getBlockChainSegmentId(blockId);
    }

    public Long getBlockHeightForBlockId(final BlockId blockId) throws DatabaseException {
        return _getBlockHeightForBlockId(blockId);
    }

    public MutableBlock getBlock(final BlockId blockId) throws DatabaseException {
        final BlockHeader blockHeader = _blockHeaderFromDatabaseConnection(blockId);

        if (blockHeader == null) { return null; }

        final List<Transaction> transactions = _getBlockTransactions(blockId);
        final MutableBlock block = new MutableBlock(blockHeader, transactions);

        if (! Util.areEqual(blockHeader.getHash(), block.getHash())) {
            Logger.log("ERROR: Unable to inflate block: " + blockHeader.getHash());
            return null;
        }

        return block;
    }

    public BlockId getChildBlockId(final BlockChainSegmentId blockChainSegmentId, final BlockId previousBlockId) throws DatabaseException {
        return _getChildBlockId(blockChainSegmentId, previousBlockId);
    }

    /**
     *
     *     E         E'
     *     |         |
     *  #4 +----D----+ #5           Height: 3
     *          |
     *          C         C''       Height: 2
     *          |         |
     *       #2 +----B----+ #3      Height: 1
     *               |
     *               A #1           Height: 0
     *
     * Block C is an ancestor of Chain #4.
     * Block E is a descendant of Chain #1.
     *
     */
    public Boolean isBlockConnectedToChain(final BlockId blockId, final BlockChainSegmentId blockChainSegmentId, final BlockRelationship blockRelationship) throws DatabaseException {
        return _isBlockConnectedToChain(blockId, blockChainSegmentId, blockRelationship);
    }

    public Sha256Hash getBlockHashFromId(final BlockId blockId) throws DatabaseException {
        return _getBlockHashFromId(blockId);
    }

    /**
     * Returns the BlockId of the nth-parent, where n is the parentCount.
     *  For instance, getAncestor(blockId, 0) returns blockId, and getAncestor(blockId, 1) returns blockId's parent.
     */
    public BlockId getAncestorBlockId(final BlockId blockId, final Integer parentCount) throws DatabaseException {
        if (blockId == null) { return null; }

        BlockId nextBlockId = blockId;
        for (int i = 0; i < parentCount; ++i) {
            final BlockHeader blockHeader = _inflateBlockHeader(nextBlockId);
            if (blockHeader == null) { return null; }

            nextBlockId = _getBlockIdFromHash(blockHeader.getPreviousBlockHash());
        }
        return nextBlockId;
    }

    /**
     * Initializes a Mutable MedianBlockTime using only blocks that have been fully validated.
     */
    public MutableMedianBlockTime initializeMedianBlockTime() throws DatabaseException {
        Sha256Hash blockHash = Util.coalesce(_getHeadBlockHash(), Block.GENESIS_BLOCK_HASH);
        return _newInitializedMedianBlockTime(_databaseConnection, blockHash);
    }

    /**
     * Initializes a Mutable MedianBlockTime using most recent block headers.
     *  The significant difference between MutableMedianBlockTime.newInitializedMedianBlockHeaderTime and MutableMedianBlockTime.newInitializedMedianBlockTime
     *  is that BlockHeaders are downloaded and validated more quickly than blocks; therefore when validating blocks
     *  MutableMedianBlockTime.newInitializedMedianBlockTime should be used, not this function.
     */
    public MutableMedianBlockTime initializeMedianBlockHeaderTime() throws DatabaseException {
        Sha256Hash blockHash = Util.coalesce(_getHeadBlockHeaderHash(), Block.GENESIS_BLOCK_HASH);
        return _newInitializedMedianBlockTime(_databaseConnection, blockHash);
    }

    /**
     * Calculates the MedianBlockTime of the provided startingBlockId.
     * NOTE: startingBlockId is exclusive. The MedianBlockTime does NOT include the provided startingBlockId; instead,
     *  it includes the MedianBlockTime.BLOCK_COUNT (11) number of blocks before the startingBlockId.
     */
    public MedianBlockTime calculateMedianBlockTime(final BlockId startingBlockId) throws DatabaseException {
        final BlockHeader startingBlock = _inflateBlockHeader(startingBlockId);
        if (startingBlock == null) { return null; }

        final Sha256Hash blockHash = startingBlock.getPreviousBlockHash();
        return _newInitializedMedianBlockTime(_databaseConnection, blockHash);
    }

    public ChainWork getChainWork(final BlockId blockId) throws DatabaseException {
        return _getChainWork(blockId);
    }

    public void repairBlock(final Block block) throws DatabaseException {
        final TransactionDatabaseManager transactionDatabaseManager = new TransactionDatabaseManager(_databaseConnection);

        final Sha256Hash blockHash = block.getHash();
        final BlockId blockId = _getBlockIdFromHash(blockHash);
        if (blockId == null) {
            Logger.log("Block not found: " + blockHash);
            return;
        }

        final BlockChainSegmentId blockChainSegmentId = _getBlockChainSegmentId(blockId);

        _updateBlockHeader(blockId, block);

        final Set<Sha256Hash> updatedTransactions = new TreeSet<Sha256Hash>();
        { // Remove transactions that do not exist in the updated block, and update ones that do not exist...
            final HashMap<Sha256Hash, Transaction> existingTransactionHashes = new HashMap<Sha256Hash, Transaction>(block.getTransactionCount());
            for (final Transaction transaction : block.getTransactions()) {
                existingTransactionHashes.put(transaction.getHash(), transaction);
            }

            final List<TransactionId> transactionIds = transactionDatabaseManager.getTransactionIds(blockId);

            for (final TransactionId transactionId : transactionIds) {
                final Sha256Hash transactionHash = transactionDatabaseManager.getTransactionHash(transactionId);

                final Boolean transactionExistsInUpdatedBlock = existingTransactionHashes.containsKey(transactionHash);

                if (transactionExistsInUpdatedBlock) {
                    final Transaction transaction = existingTransactionHashes.get(transactionHash);
                    Logger.log("Updating Transaction: " + transactionHash + " Id: " + transactionId);
                    transactionDatabaseManager.updateTransaction(transaction);
                    updatedTransactions.add(transactionHash);
                }
                else {
                    Logger.log("Deleting Transaction: " + transactionHash + " Id: " + transactionId);
                    transactionDatabaseManager.deleteTransaction(transactionId);
                }
            }
        }

        for (final Transaction transaction : block.getTransactions()) {
            final Sha256Hash transactionHash = transaction.getHash();
            final Boolean transactionHasBeenProcessed = updatedTransactions.contains(transactionHash);
            if (transactionHasBeenProcessed) { continue; }

            Logger.log("Inserting Transaction: " + transactionHash);
            final TransactionId transactionId = transactionDatabaseManager.insertTransaction(transaction);
            if (transactionId == null) { throw new DatabaseException("Error inserting Transaction."); }
        }
    }

    public BlockId getBlockIdAtHeight(final BlockChainSegmentId blockChainSegmentId, final Long blockHeight) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM blocks WHERE block_height = ?")
                .setParameter(blockHeight)
        );

        for (final Row row : rows) {
            final BlockId blockId = BlockId.wrap(row.getLong("id"));

            final Boolean blockIsConnectedToChain = _isBlockConnectedToChain(blockId, blockChainSegmentId, BlockRelationship.ANY);
            if (blockIsConnectedToChain) {
                return blockId;
            }
        }

        return null;
    }
}
