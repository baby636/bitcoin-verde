package com.softwareverde.bitcoin.server.module.node.database.transaction.fullnode;

import com.softwareverde.bitcoin.block.Block;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.timer.MilliTimer;

public class UnspentTransactionOutputCommitter {
    protected final FullNodeTransactionDatabaseManager _transactionDatabaseManager;

    public UnspentTransactionOutputCommitter(final FullNodeTransactionDatabaseManager transactionDatabaseManager) {
        _transactionDatabaseManager = transactionDatabaseManager;
    }

    public void commitUnspentTransactionOutputs(final Block block, final Long blockHeight, final DatabaseConnectionFactory databaseConnectionFactory) throws DatabaseException {
        final MilliTimer totalTimer = new MilliTimer();
        final MilliTimer utxoCommitTimer = new MilliTimer();
        final MilliTimer utxoTimer = new MilliTimer();

        totalTimer.start();

        final List<Transaction> transactions = block.getTransactions();
        final int transactionCount = transactions.getCount();

        final MutableList<TransactionOutputIdentifier> spentTransactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>();
        final MutableList<TransactionOutputIdentifier> unspentTransactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>();
        for (int i = 0; i < transactions.getCount(); ++i) {
            final Transaction transaction = transactions.get(i);

            final boolean isCoinbase = (i == 0);
            if (! isCoinbase) {
                for (final TransactionInput transactionInput : transaction.getTransactionInputs()) {
                    final TransactionOutputIdentifier transactionOutputIdentifier = TransactionOutputIdentifier.fromTransactionInput(transactionInput);
                    spentTransactionOutputIdentifiers.add(transactionOutputIdentifier);
                }
            }

            final List<TransactionOutputIdentifier> transactionOutputIdentifiers = TransactionOutputIdentifier.fromTransactionOutputs(transaction);
            unspentTransactionOutputIdentifiers.addAll(transactionOutputIdentifiers);
        }

        final int worstCaseNewUtxoCount = (unspentTransactionOutputIdentifiers.getCount() + spentTransactionOutputIdentifiers.getCount());
        final Long uncommittedUtxoCount = _transactionDatabaseManager.getCachedUnspentTransactionOutputCount();
        if ( ((blockHeight % 2016L) == 0L) || ( (uncommittedUtxoCount + worstCaseNewUtxoCount) >= UtxoDatabaseManager.DEFAULT_MAX_UTXO_CACHE_COUNT) ) {
            utxoCommitTimer.start();
            _transactionDatabaseManager.commitUnspentTransactionOutputs(databaseConnectionFactory);
            utxoCommitTimer.stop();
            Logger.debug("Commit Timer: " + utxoCommitTimer.getMillisecondsElapsed() + "ms.");
        }

        utxoTimer.start();
        FullNodeTransactionDatabaseManager.UTXO_WRITE_MUTEX.lock();
        try {
            _transactionDatabaseManager.insertUnspentTransactionOutputs(unspentTransactionOutputIdentifiers, blockHeight);
            _transactionDatabaseManager.markTransactionOutputsAsSpent(spentTransactionOutputIdentifiers);
        }
        finally {
            FullNodeTransactionDatabaseManager.UTXO_WRITE_MUTEX.unlock();
        }

        utxoTimer.stop();
        totalTimer.stop();

        Logger.debug("BlockHeight: " + blockHeight + " " + unspentTransactionOutputIdentifiers.getCount() + " unspent, " + spentTransactionOutputIdentifiers.getCount() + " spent. " + transactionCount + " in " + totalTimer.getMillisecondsElapsed() + " ms (" + (transactionCount * 1000L / (totalTimer.getMillisecondsElapsed() + 1L)) + " tps) " + utxoTimer.getMillisecondsElapsed() + "ms UTXO " + (transactions.getCount() * 1000L / (utxoTimer.getMillisecondsElapsed() + 1L)) + " tps");


    }
}
