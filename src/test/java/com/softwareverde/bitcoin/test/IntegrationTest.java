package com.softwareverde.bitcoin.test;

import com.softwareverde.bitcoin.block.BlockId;
import com.softwareverde.bitcoin.chain.time.MedianBlockTime;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.bitcoin.server.database.ReadUncommittedDatabaseConnectionFactoryWrapper;
import com.softwareverde.bitcoin.server.database.cache.DatabaseManagerCache;
import com.softwareverde.bitcoin.server.database.cache.DisabledDatabaseManagerCache;
import com.softwareverde.bitcoin.server.database.pool.DatabaseConnectionPool;
import com.softwareverde.bitcoin.server.main.BitcoinVerdeDatabase;
import com.softwareverde.bitcoin.server.main.NativeUnspentTransactionOutputCache;
import com.softwareverde.bitcoin.server.module.node.database.block.header.fullnode.FullNodeBlockHeaderDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.fullnode.FullNodeDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.fullnode.FullNodeDatabaseManagerFactory;
import com.softwareverde.bitcoin.server.module.node.database.spv.SpvDatabaseManagerFactory;
import com.softwareverde.concurrent.pool.MainThreadPool;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.DatabaseInitializer;
import com.softwareverde.database.mysql.MysqlDatabaseConnection;
import com.softwareverde.database.mysql.MysqlDatabaseConnectionFactory;
import com.softwareverde.database.mysql.MysqlDatabaseInitializer;
import com.softwareverde.database.mysql.connection.ReadUncommittedDatabaseConnectionFactory;
import com.softwareverde.logging.Logger;
import com.softwareverde.test.database.MysqlTestDatabase;
import com.softwareverde.test.database.TestDatabase;

import java.sql.Connection;

public class IntegrationTest extends UnitTest {
    protected static final TestDatabase _database = new TestDatabase(new MysqlTestDatabase());
    protected static final Boolean _nativeCacheIsEnabled = NativeUnspentTransactionOutputCache.isEnabled();
    protected static Boolean _nativeCacheWasInitialized = false;

    protected final DatabaseManagerCache _databaseManagerCache = new DisabledDatabaseManagerCache();
    protected final MainThreadPool _threadPool = new MainThreadPool(1, 1L);

    protected final FullNodeDatabaseManagerFactory _fullNodeDatabaseManagerFactory;
    protected final FullNodeDatabaseManagerFactory _readUncommittedDatabaseManagerFactory;
    protected final SpvDatabaseManagerFactory _spvDatabaseManagerFactory;

    public IntegrationTest() {
        final DatabaseConnectionFactory databaseConnectionFactory = _database.getDatabaseConnectionFactory();
        _fullNodeDatabaseManagerFactory = new FullNodeDatabaseManagerFactory(databaseConnectionFactory, _databaseManagerCache);
        _spvDatabaseManagerFactory = new SpvDatabaseManagerFactory(databaseConnectionFactory, _databaseManagerCache);

        final ReadUncommittedDatabaseConnectionFactory readUncommittedDatabaseConnectionFactory = new ReadUncommittedDatabaseConnectionFactoryWrapper(databaseConnectionFactory);
        _readUncommittedDatabaseManagerFactory = new FullNodeDatabaseManagerFactory(readUncommittedDatabaseConnectionFactory, _databaseManagerCache);

        // Bypass the Hikari database connection pool...
        _database.setDatabaseConnectionPool(new DatabaseConnectionPool() {
            protected final MutableList<DatabaseConnection> _databaseConnections = new MutableList<DatabaseConnection>();

            @Override
            public DatabaseConnection newConnection() throws DatabaseException {
                final DatabaseConnection databaseConnection = databaseConnectionFactory.newConnection();
                _databaseConnections.add(databaseConnection);
                return databaseConnection;
            }

            @Override
            public void close() throws DatabaseException {
                try {
                    for (final DatabaseConnection databaseConnection : _databaseConnections) {
                        databaseConnection.close();
                    }
                }
                finally {
                    _databaseConnections.clear();
                }
            }
        });

    }

    static {
        _resetDatabase();
    }

    protected static void _resetDatabase() {
        final DatabaseInitializer<Connection> databaseInitializer = new MysqlDatabaseInitializer("sql/full_node/init_mysql.sql", 2, BitcoinVerdeDatabase.DATABASE_UPGRADE_HANDLER);
        try {
            _database.reset();

            final MysqlDatabaseConnectionFactory databaseConnectionFactory = _database.getMysqlDatabaseConnectionFactory();
            try (final MysqlDatabaseConnection databaseConnection = databaseConnectionFactory.newConnection()) {
                databaseInitializer.initializeDatabase(databaseConnection);
            }

            if (_nativeCacheIsEnabled) {
                if (_nativeCacheWasInitialized) {
                    NativeUnspentTransactionOutputCache.destroy();
                }
                NativeUnspentTransactionOutputCache.init();
                _nativeCacheWasInitialized = true;
            }
            else {
                Logger.info("NOTICE: NativeUtxoCache not enabled.");
            }
        }
        catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    protected FullNodeDatabaseManagerFactory _getDatabaseManagerFactoryThatFakesMedianBlocksTimesWhenNotCalculable() {
        final DatabaseConnectionFactory databaseConnectionFactory = _database.getDatabaseConnectionFactory();
        return _getDatabaseManagerFactoryThatFakesMedianBlocksTimesWhenNotCalculable(databaseConnectionFactory, _databaseManagerCache);
    }

    protected FullNodeDatabaseManagerFactory _getReadUncommittedDatabaseManagerFactoryThatFakesMedianBlockTimesWhenNotCalculable() {
        final DatabaseConnectionFactory databaseConnectionFactory = _database.getDatabaseConnectionFactory();
        final ReadUncommittedDatabaseConnectionFactory readUncommittedDatabaseConnectionFactory = new ReadUncommittedDatabaseConnectionFactoryWrapper(databaseConnectionFactory);
        return _getDatabaseManagerFactoryThatFakesMedianBlocksTimesWhenNotCalculable(readUncommittedDatabaseConnectionFactory, _databaseManagerCache);
    }

    protected FullNodeDatabaseManagerFactory _getDatabaseManagerFactoryThatFakesMedianBlocksTimesWhenNotCalculable(final DatabaseConnectionFactory databaseConnectionFactory, final DatabaseManagerCache databaseManagerCache) {
        return new FullNodeDatabaseManagerFactory(databaseConnectionFactory, databaseManagerCache) {
            @Override
            public FullNodeDatabaseManager newDatabaseManager() throws DatabaseException {
                final DatabaseConnection databaseConnection = _databaseConnectionFactory.newConnection();
                return new FullNodeDatabaseManager(databaseConnection, _databaseManagerCache) {
                    @Override
                    public FullNodeBlockHeaderDatabaseManager getBlockHeaderDatabaseManager() {
                        if (_blockHeaderDatabaseManager == null) {
                            _blockHeaderDatabaseManager = new FullNodeBlockHeaderDatabaseManager(this) {
                                @Override
                                public MedianBlockTime calculateMedianBlockTime(final BlockId blockId) throws DatabaseException {
                                    try {
                                        return super.calculateMedianBlockTime(blockId);
                                    }
                                    catch (Exception ignored) {
                                        return MedianBlockTime.fromSeconds(MedianBlockTime.GENESIS_BLOCK_TIMESTAMP);
                                    }
                                }

                                @Override
                                public MedianBlockTime calculateMedianBlockTimeStartingWithBlock(final BlockId blockId) throws DatabaseException {
                                    try {
                                        return super.calculateMedianBlockTimeStartingWithBlock(blockId);
                                    }
                                    catch (Exception ignored) {
                                        return MedianBlockTime.fromSeconds(MedianBlockTime.GENESIS_BLOCK_TIMESTAMP);
                                    }
                                }
                            };
                        }

                        return _blockHeaderDatabaseManager;
                    }
                };
            }
        };
    }
}
