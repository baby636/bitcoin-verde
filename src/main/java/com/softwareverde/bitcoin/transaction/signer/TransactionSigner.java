package com.softwareverde.bitcoin.transaction.signer;

import com.softwareverde.bitcoin.secp256k1.Secp256k1;
import com.softwareverde.bitcoin.secp256k1.signature.Signature;
import com.softwareverde.bitcoin.transaction.MutableTransaction;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionDeflater;
import com.softwareverde.bitcoin.transaction.input.MutableTransactionInput;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.output.MutableTransactionOutput;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.script.MutableScript;
import com.softwareverde.bitcoin.transaction.script.Script;
import com.softwareverde.bitcoin.transaction.script.ScriptBuilder;
import com.softwareverde.bitcoin.transaction.script.locking.LockingScript;
import com.softwareverde.bitcoin.transaction.script.opcode.Opcode;
import com.softwareverde.bitcoin.transaction.script.signature.ScriptSignature;
import com.softwareverde.bitcoin.transaction.script.signature.hashtype.HashType;
import com.softwareverde.bitcoin.transaction.script.signature.hashtype.Mode;
import com.softwareverde.bitcoin.transaction.script.unlocking.UnlockingScript;
import com.softwareverde.bitcoin.type.key.PrivateKey;
import com.softwareverde.bitcoin.type.key.PublicKey;
import com.softwareverde.bitcoin.util.BitcoinUtil;
import com.softwareverde.bitcoin.util.ByteUtil;
import com.softwareverde.util.bytearray.ByteArrayBuilder;
import com.softwareverde.util.bytearray.Endian;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.util.HexUtil;
import com.softwareverde.util.Util;

public class TransactionSigner {
    private static final byte[] INVALID_SIGNATURE_HASH_SINGLE_VALUE = HexUtil.hexStringToByteArray("0100000000000000000000000000000000000000000000000000000000000000");

    // Steps:
    // 1. Set all input-scripts to empty scripts.
    // 2. Set the input's (associated with the inputIndexToBeSigned) unlocking-script to the value of its corresponding output-script from the previous transaction.
    // 3. Append the signatureHashType byte to the serialized transaction bytes.
    // 4. Hash the transaction twice.
    protected byte[] _getBytesForSigning(final SignatureContext signatureContext) {
        final TransactionDeflater transactionDeflater = new TransactionDeflater();

        final Transaction transaction = signatureContext.getTransaction();
        // NOTE: The if the currentScript has not been set, the current script will default to the PreviousTransactionOutput's locking script.
        // if (currentScript == null) { throw new NullPointerException("SignatureContext must have its currentScript set."); }

        final List<TransactionInput> transactionInputs = transaction.getTransactionInputs();
        final List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();

        final HashType hashType = signatureContext.getHashType();
        final Mode signatureMode = hashType.getMode();

        { // Bitcoin Core Bug: https://bitcointalk.org/index.php?topic=260595.0
            // This bug is caused when an input uses SigHash Single without a matching output.
            // Originally, the Bitcoin Core client returned "1" as the bytes to be hashed, but the invoker never checked
            // for that case, which caused the "1" value to be the actual bytes that are signed for the whole transaction.
            if (signatureMode == Mode.SIGNATURE_HASH_SINGLE) {
                if (signatureContext.getInputIndexBeingSigned() >= transactionOutputs.getSize()) {
                    return INVALID_SIGNATURE_HASH_SINGLE_VALUE;
                }
            }
        }

        final MutableTransaction mutableTransaction = new MutableTransaction();
        mutableTransaction.setVersion(transaction.getVersion());
        mutableTransaction.setHasWitnessData(transaction.hasWitnessData());
        mutableTransaction.setLockTime(transaction.getLockTime());

        for (int inputIndex = 0; inputIndex < transactionInputs.getSize(); ++inputIndex) {
            if (! signatureContext.shouldInputBeSigned(inputIndex)) { continue; }

            final TransactionInput transactionInput = transactionInputs.get(inputIndex);

            final MutableTransactionInput mutableTransactionInput = new MutableTransactionInput();
            mutableTransactionInput.setPreviousOutputIndex(transactionInput.getPreviousOutputIndex());
            mutableTransactionInput.setPreviousOutputTransactionHash(transactionInput.getPreviousOutputTransactionHash());

            { // Handle Input-Script Signing...
                final Script unlockingScriptForSigning;
                final Boolean shouldSignScript = signatureContext.shouldInputScriptBeSigned(inputIndex);
                if (shouldSignScript) {
                    final Script currentScript = signatureContext.getCurrentScript();
                    final TransactionOutput transactionOutputBeingSpent = signatureContext.getTransactionOutputBeingSpent(inputIndex);
                    final LockingScript outputBeingSpentLockingScript = transactionOutputBeingSpent.getLockingScript();

                    { // Handle Code-Separators...
                        final Integer subscriptIndex = signatureContext.getLastCodeSeparatorIndex(inputIndex);
                        if (subscriptIndex > 0) {
                            final MutableScript mutableScript = new MutableScript(Util.coalesce(currentScript, outputBeingSpentLockingScript));
                            mutableScript.subScript(subscriptIndex);
                            mutableScript.removeOperations(Opcode.CODE_SEPARATOR);
                            unlockingScriptForSigning = mutableScript;
                        }
                        else {
                            unlockingScriptForSigning = Util.coalesce(currentScript, outputBeingSpentLockingScript);
                        }
                    }
                }
                else {
                    unlockingScriptForSigning = UnlockingScript.EMPTY_SCRIPT;
                }

                { // Remove any ByteArrays that should be excluded from the script signing (aka signatures)...
                    final MutableScript modifiedScript = new MutableScript(unlockingScriptForSigning);
                    final List<ByteArray> bytesToExcludeFromScript = signatureContext.getBytesToExcludeFromScript();
                    for (final ByteArray byteArray : bytesToExcludeFromScript) {
                        modifiedScript.removePushOperations(byteArray);
                    }
                    mutableTransactionInput.setUnlockingScript(UnlockingScript.castFrom(modifiedScript));
                }
            }

            { // Handle Input-Sequence-Number Signing...
                if (signatureContext.shouldInputSequenceNumberBeSigned(inputIndex)) {
                    mutableTransactionInput.setSequenceNumber(transactionInput.getSequenceNumber());
                }
                else {
                    mutableTransactionInput.setSequenceNumber(0L);
                }
            }

            mutableTransaction.addTransactionInput(mutableTransactionInput);
        }

        for (int outputIndex = 0; outputIndex < transactionOutputs.getSize(); ++outputIndex) {
            if (! signatureContext.shouldOutputBeSigned(outputIndex)) { continue; } // If the output should not be signed, then it is omitted from the signature completely...

            final TransactionOutput transactionOutput = transactionOutputs.get(outputIndex);
            final MutableTransactionOutput mutableTransactionOutput = new MutableTransactionOutput();

            { // Handle Output-Amounts Signing...
                if (signatureContext.shouldOutputAmountBeSigned(outputIndex)) {
                    mutableTransactionOutput.setAmount(transactionOutput.getAmount());
                }
                else {
                    mutableTransactionOutput.setAmount(-1L);
                }
            }

            { // Handle Output-Script Signing...
                if (signatureContext.shouldOutputScriptBeSigned(outputIndex)) {
                    mutableTransactionOutput.setLockingScript(transactionOutput.getLockingScript());
                }
                else {
                    mutableTransactionOutput.setLockingScript(LockingScript.EMPTY_SCRIPT);
                }
            }

            mutableTransactionOutput.setIndex(transactionOutput.getIndex());
            mutableTransaction.addTransactionOutput(mutableTransactionOutput);
        }

        final ByteArrayBuilder byteArrayBuilder = transactionDeflater.toByteArrayBuilder(mutableTransaction);
        byteArrayBuilder.appendBytes(ByteUtil.integerToBytes(ByteUtil.byteToInteger(hashType.toByte())), Endian.LITTLE);
        final byte[] bytes = byteArrayBuilder.build();

        return BitcoinUtil.sha256(BitcoinUtil.sha256(bytes));
    }

    public boolean isSignatureValid(final SignatureContext signatureContext, final PublicKey publicKey, final ScriptSignature scriptSignature) {
        final byte[] bytesForSigning = _getBytesForSigning(signatureContext);
        return Secp256k1.verifySignature(scriptSignature.getSignature(), publicKey, bytesForSigning);
    }

    public Transaction signTransaction(final SignatureContext signatureContext, final PrivateKey privateKey) {
        // NOTE: ensure signatureContext has its lastCodeSeparatorIndex set.

        final MutableTransaction mutableTransaction = new MutableTransaction(signatureContext.getTransaction());
        final byte[] bytesToSign = _getBytesForSigning(signatureContext);
        final Signature signature = Secp256k1.sign(privateKey.getBytes(), bytesToSign);
        final ScriptSignature scriptSignature = new ScriptSignature(signature, signatureContext.getHashType());

        final List<TransactionInput> transactionInputs = mutableTransaction.getTransactionInputs();
        for (int i = 0; i < transactionInputs.getSize(); ++i) {
            final TransactionInput transactionInput = transactionInputs.get(i);

            if (signatureContext.shouldInputScriptBeSigned(i)) {
                final MutableTransactionInput mutableTransactionInput = new MutableTransactionInput(transactionInput);
                mutableTransactionInput.setUnlockingScript(ScriptBuilder.unlockPayToAddress(scriptSignature, privateKey.getPublicKey()));
                mutableTransaction.setTransactionInput(i, mutableTransactionInput);
            }
        }

        return mutableTransaction;
    }
}
