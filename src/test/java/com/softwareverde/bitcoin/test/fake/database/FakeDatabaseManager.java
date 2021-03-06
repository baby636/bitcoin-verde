package com.softwareverde.bitcoin.test.fake.database;

import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.cache.DatabaseManagerCache;
import com.softwareverde.bitcoin.server.module.node.database.DatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.block.BlockDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.block.header.BlockHeaderDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.block.pending.PendingBlockDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.blockchain.BlockchainDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.node.BitcoinNodeDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.transaction.TransactionDatabaseManager;
import com.softwareverde.database.DatabaseException;

public interface FakeDatabaseManager extends DatabaseManager {
    @Override
    default DatabaseConnection getDatabaseConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    default DatabaseManagerCache getDatabaseManagerCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    default BitcoinNodeDatabaseManager getNodeDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default BlockchainDatabaseManager getBlockchainDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default BlockDatabaseManager getBlockDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default BlockHeaderDatabaseManager getBlockHeaderDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default PendingBlockDatabaseManager getPendingBlockDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default TransactionDatabaseManager getTransactionDatabaseManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void close() throws DatabaseException { }
}