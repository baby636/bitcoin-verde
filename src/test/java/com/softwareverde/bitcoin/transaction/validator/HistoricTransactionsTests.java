package com.softwareverde.bitcoin.transaction.validator;

import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.input.TransactionInputInflater;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.TransactionOutputInflater;
import com.softwareverde.bitcoin.transaction.script.Script;
import com.softwareverde.bitcoin.transaction.script.locking.ImmutableLockingScript;
import com.softwareverde.bitcoin.transaction.script.locking.LockingScript;
import com.softwareverde.bitcoin.transaction.script.opcode.CryptographicOperation;
import com.softwareverde.bitcoin.transaction.script.opcode.Operation;
import com.softwareverde.bitcoin.transaction.script.opcode.OperationInflater;
import com.softwareverde.bitcoin.transaction.script.runner.ScriptRunner;
import com.softwareverde.bitcoin.transaction.script.runner.context.MutableContext;
import com.softwareverde.bitcoin.transaction.script.stack.Stack;
import com.softwareverde.bitcoin.transaction.script.stack.Value;
import com.softwareverde.bitcoin.transaction.script.unlocking.ImmutableUnlockingScript;
import com.softwareverde.bitcoin.transaction.script.unlocking.UnlockingScript;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.constable.list.List;
import com.softwareverde.util.HexUtil;
import org.junit.Assert;
import org.junit.Test;

public class HistoricTransactionsTests {
    @Test
    public void should_verify_multisig_transaction_EB3B82C0884E3EFA6D8B0BE55B4915EB20BE124C9766245BCC7F34FDAC32BCCB_1() throws Exception {
        // Setup
        final Stack stack = new Stack();
        {
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("00000000")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("30440220276D6DAD3DEFA37B5F81ADD3992D510D2F44A317FD85E04F93A1E2DAEA64660202200F862A0DA684249322CEB8ED842FB8C859C0CB94C81E1C5308B4868157A428EE01")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("01000000")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("0232ABDC893E7F0631364D7FD01CB33D24DA45329A00357B3A7886211AB414D55A")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("01000000")));
        }

        final MutableContext mutableContext = new MutableContext();
        {
            final TransactionInflater transactionInflater = new TransactionInflater();
            final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000024DE8B0C4C2582DB95FA6B3567A989B664484C7AD6672C85A3DA413773E63FDB8000000006B48304502205B282FBC9B064F3BC823A23EDCC0048CBB174754E7AA742E3C9F483EBE02911C022100E4B0B3A117D36CAB5A67404DDDBF43DB7BEA3C1530E0FE128EBC15621BD69A3B0121035AA98D5F77CD9A2D88710E6FC66212AFF820026F0DAD8F32D1F7CE87457DDE50FFFFFFFF4DE8B0C4C2582DB95FA6B3567A989B664484C7AD6672C85A3DA413773E63FDB8010000006F004730440220276D6DAD3DEFA37B5F81ADD3992D510D2F44A317FD85E04F93A1E2DAEA64660202200F862A0DA684249322CEB8ED842FB8C859C0CB94C81E1C5308B4868157A428EE01AB51210232ABDC893E7F0631364D7FD01CB33D24DA45329A00357B3A7886211AB414D55A51AEFFFFFFFF02E0FD1C00000000001976A914380CB3C594DE4E7E9B8E18DB182987BEBB5A4F7088ACC0C62D000000000017142A9BC5447D664C1D0141392A842D23DBA45C4F13B17500000000"));
            final Integer transactionInputIndex = 1;

            final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
            final Integer txOutIndex = 1;
            final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(txOutIndex, HexUtil.hexStringToByteArray("C0C62D000000000017142A9BC5447D664C1D0141392A842D23DBA45C4F13B175"));
            final Integer codeSeparatorIndex = 3;

            final Script unlockingScript;
            {
                final List<TransactionInput> transactionInputs = transaction.getTransactionInputs();
                final TransactionInput transactionInput = transactionInputs.get(transactionInputIndex);
                unlockingScript = transactionInput.getUnlockingScript();
            }

            mutableContext.setCurrentScript(unlockingScript); // Set the current script, since ScriptRunner isn't being used here...
            mutableContext.setTransaction(transaction);
            mutableContext.setTransactionInputIndex(transactionInputIndex);
            mutableContext.setTransactionOutput(transactionOutput);
            mutableContext.setLockingScriptLastCodeSeparatorIndex(codeSeparatorIndex);
        }

        // final CryptographicOperation checkMultisigOperation = new CryptographicOperation((byte) 0xAE, Operation.Opcode.CHECK_MULTISIGNATURE);
        final OperationInflater operationInflater = new OperationInflater();
        final Operation checkMultisigOperation = operationInflater.fromBytes(new ByteArrayReader(new byte[] { (byte) 0xAE }));
        Assert.assertTrue(checkMultisigOperation instanceof CryptographicOperation);

        // Action
        final Boolean shouldContinue = checkMultisigOperation.applyTo(stack, mutableContext);
        final Value lastValue = stack.pop();

        // Assert
        Assert.assertTrue(shouldContinue);
        Assert.assertFalse(stack.didOverflow());
        Assert.assertTrue(lastValue.asBoolean());
    }

    @Test
    public void should_verify_SIGHASHNONE_transaction_599E47A8114FE098103663029548811D2651991B62397E057F0C863C2BC9F9EA_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000015F386C8A3842C9A9DCFA9B78BE785A40A7BDA08B64646BE3654301EACCFC8D5E010000008A4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6FFFFFFFF0240420F00000000001976A914660D4EF3A743E3E696AD990364E555C271AD504B88AC2072C801000000001976A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("5F386C8A3842C9A9DCFA9B78BE785A40A7BDA08B64646BE3654301EACCFC8D5E010000008A4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6FFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("60B4D701000000001976A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(178627L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_6A26D2ECB67F27D1FA5524763B49029D7106E91E3CC05743073461A719776192_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000001F6EA284EC7521F8A7D094A6CF4E6873098B90F90725FFD372B343189D7A4089C0100000026255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AEFFFFFFFF0130570500000000001976A9145A3ACBC7BBCC97C5FF16F5909C9D7D3FADB293A888AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("F6EA284EC7521F8A7D094A6CF4E6873098B90F90725FFD372B343189D7A4089C0100000026255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("801A06000000000017A91419A7D869032368FD1F1E26E5E73A4AD0E474960E87"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(170095L); // NOTE: P2SH was not activated until Block 173805...
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A91419A7D869032368FD1F1E26E5E73A4AD0E474960E87"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_1CC1ECDF5C05765DF3D1F59FBA24CD01C45464C329B0F0A25AA9883ADFCF7F29_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000014ACC6214C74AF23AE430AFF165004D8E10DB9860B87FFB7E7E703AF97B872203010000009200483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AEFFFFFFFF02C0175302000000001976A9146723D3398B384F0C0A8F717C100905F36E2ED7D488AC80969800000000001976A9148A033817801503863C9D6BD124D153F8407F2B4188AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("4ACC6214C74AF23AE430AFF165004D8E10DB9860B87FFB7E7E703AF97B872203010000009200483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("80F0FA020000000017A9145C02C49641699863F909BF4BF3BE8398D2E383F187"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(177644L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A9145C02C49641699863F909BF4BF3BE8398D2E383F187"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("00483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_968A692AB98B1F275C635C76BE003AB1DB9740D0B62F338B270115342CA42F5B_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("010000000120DA26875149D4F80F3C36ABC7F984720ED14F6396854AD1ABCA4FE72C88DC4F00000000FDFD0000483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AEFFFFFFFF0330C11D00000000001976A914FD5E323C595B2614F47D6BE25BA079F081628C9B88AC80969800000000001976A91406F1B67078FC400A63D54C313CD6BB817E4760F088AC40787D01000000001976A9140231C76FF14600B49F7C1B734A69F169C7BA1BAC88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("20DA26875149D4F80F3C36ABC7F984720ED14F6396854AD1ABCA4FE72C88DC4F00000000FDFD0000483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("409334020000000017A91493DD75558893D97C53005F6B63B9E4005401A93187"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(177653L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A91493DD75558893D97C53005F6B63B9E4005401A93187"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("00483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_0ADF1D18C0620CF2AF88DF250402A3D0E5CD90BC081E5BFF0EEE0631A9B232C0_2() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000045930CB11E0447A833C4EB510494D0DB9A73B8489085002806B4938DAD91A4D4C010000008B483045022100C47CCD574FEA4677B942CC2A5FD49D44CB9827996A090D86BBFEC14286C158D3022067377B328848AF6718BE8ED4F7DA7841DEFAFE9CA013DC5ED4FCD7BD86F2698601410459ED4A6929F45F4C40ED221DE16ADEBA13AD9F35E47C69DC1081F7F3540539DE2C85CA8A5B2DB1995B18FB1F1D54EE1183775A5362AD6B85A74975671006253DFFFFFFFFB6A82EAA6E0A93E5233FCD34AF2375E42C72BA6711CDCD672819F99AF781A8A5010000008C493046022100CCC7758371F7B9E301183C28696FA721A2E3617D50A3761C6A19D94408C4E753022100D54835B7B08C38A3992A69EED47717AFDC24738B9A81FB311536EC7AD8712613014104E1E5C8ADCA296F2EBBDA9C19FFC7D31732787A75182FF8BEE7EA51C2CD0D20C1CB26E008E49BC9D937D42D15C9686184068DEB9668C8B39F1F6C5D5E3BEB10DEFFFFFFFFB2A2B35A2750E5203820BFE92166E317E1A797327AED713BEE90FD8E2159453F010000008A4730440220710CAA1EBACA07EE4C8D055F57E4967B3948CBD2DBCA0BA002DEC8CA36BEBB0002200032F44BEDB3ABA4AD0CCA5E302B77D6D279B8A9EE3F66D02D5D0EC1BFB89E3E014104C33859AF99F7223FBF819F941143AC90D5DBCCCCF00B2FFAA6A1709F848AEFEF355019643D5727A5FFCFA594F870FB36D64506E6EEEC69624FB1719871CA4247FFFFFFFFAE49869B9ED83890F573211552504753756718332556CD35D535D67B81075EB0010000008B4830450221009DFF4FD759A1ABDCAF713F12465967AF66721604EE75812D61FA73DC633B924702203463AF5D7F1A098F8D705CFFF6AD0864434D526EABF6841F39C6DDA5FECC3754014104EC698E34FBB29B78B49C4574D8EAC51F292E0F72790AD3BC6AF856874715D68FB449E8FD4B05C7313C412C3DBF6788D00005E35C4DDA5A0F832343077520D7B5FFFFFFFF0200E721A2040000001976A91424DD0E523B29A97C039C4F3D605E969E38AFA02488ACF2DDEA4C000000001976A914B9887A490A7CE1CE00610EBC50CB629C02B1C57488AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("4730440220710CAA1EBACA07EE4C8D055F57E4967B3948CBD2DBCA0BA002DEC8CA36BEBB0002200032F44BEDB3ABA4AD0CCA5E302B77D6D279B8A9EE3F66D02D5D0EC1BFB89E3E014104C33859AF99F7223FBF819F941143AC90D5DBCCCCF00B2FFAA6A1709F848AEFEF355019643D5727A5FFCFA594F870FB36D64506E6EEEC69624FB1719871CA4247"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("18E94C0C000000001976A9141A49982E41F9B53E6FCDDEEE6EE542BDA4E424DC88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(169940L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(2);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A9141A49982E41F9B53E6FCDDEEE6EE542BDA4E424DC88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("4730440220710CAA1EBACA07EE4C8D055F57E4967B3948CBD2DBCA0BA002DEC8CA36BEBB0002200032F44BEDB3ABA4AD0CCA5E302B77D6D279B8A9EE3F66D02D5D0EC1BFB89E3E014104C33859AF99F7223FBF819F941143AC90D5DBCCCCF00B2FFAA6A1709F848AEFEF355019643D5727A5FFCFA594F870FB36D64506E6EEEC69624FB1719871CA4247"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_51BF528ECF3C161E7C021224197DBE84F9A8564212F6207BAA014C01A1668E1E_0() {
        // NOTE: This transaction relies on SIGHASH_ANYONECANPAY...

        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000002F6044C0AD485F633B41F97D0D793EB2837AE40F738FF6D5F50FDFD10528C1D76010000006B48304502205853C7F1395785BFABB03C57E962EB076FF24D8E4E573B04DB13B45ED3ED6EE20221009DC82AE43BE9D4B1FE2847754E1D36DAD48BA801817D485DC529AFC516C2DDB481210305584980367B321FAD7F1C1F4D5D723D0AC80C1D80C8BA12343965B48364537AFFFFFFFF9C6AF0DF6669BCDED19E317E25BEBC8C78E48DF8AE1FE02A7F030818E71ECD40010000006C4930460221008269C9D7BA0A7E730DD16F4082D29E3684FB7463BA064FD093AFC170AD6E0388022100BC6D76373916A3FF6EE41B2C752001FDA3C9E048BCFF0D81D05B39FF0F4217B2812103AAE303D825421545C5BC7CCD5AC87DD5ADD3BCC3A432BA7AA2F2661699F9F659FFFFFFFF01E0930400000000001976A9145C11F917883B927EEF77DC57707AEB853F6D389488AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("F6044C0AD485F633B41F97D0D793EB2837AE40F738FF6D5F50FDFD10528C1D76010000006B48304502205853C7F1395785BFABB03C57E962EB076FF24D8E4E573B04DB13B45ED3ED6EE20221009DC82AE43BE9D4B1FE2847754E1D36DAD48BA801817D485DC529AFC516C2DDB481210305584980367B321FAD7F1C1F4D5D723D0AC80C1D80C8BA12343965B48364537AFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("400D0300000000001976A9148551E48A53DECD1CFC63079A4581BCCCFAD1A93C88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(207733L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A9148551E48A53DECD1CFC63079A4581BCCCFAD1A93C88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("48304502205853C7F1395785BFABB03C57E962EB076FF24D8E4E573B04DB13B45ED3ED6EE20221009DC82AE43BE9D4B1FE2847754E1D36DAD48BA801817D485DC529AFC516C2DDB481210305584980367B321FAD7F1C1F4D5D723D0AC80C1D80C8BA12343965B48364537A"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_7208E5EDF525F04E705FB3390194E316205B8F995C8C9FCD8C6093ABE04FA27D_0() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000001774431E6CCCA53548400E2E8BB66865CA2FEEF7885F09A0199B5FDD0EEB7583E01000000DC004930460221008662E2BFCC4741D92531F566C5765D849E24B45C289607593584FBBA938D88CC022100D718E710D5991BDFBDC5B0D52AE8E4D9C1C1C719BCC348003C5E80524A04E16283483045022100FBDBEA6B614989B210D269DBF171746A9507BB3DAE292BDAF85848A7AA091ECA02205D23A46269A904C40076C76EADFDB8F98E7D0349C0BFE5915CCA3F8835A4A41683475221034758CEFCB75E16E4DFAFB32383B709FA632086EA5CA982712DE6ADD93060B17A2103FE96237629128A0AE8C3825AF8A4BE8FE3109B16F62AF19CEC0B1EB93B8717E252AEFFFFFFFF0280969800000000001976A914F164A82C9B3C5D217C83380792D56A6261F2D17688AC609DF200000000001976A9142AB55D985E552653C189B1530AAC817C0223CB4C88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("774431E6CCCA53548400E2E8BB66865CA2FEEF7885F09A0199B5FDD0EEB7583E01000000DC004930460221008662E2BFCC4741D92531F566C5765D849E24B45C289607593584FBBA938D88CC022100D718E710D5991BDFBDC5B0D52AE8E4D9C1C1C719BCC348003C5E80524A04E16283483045022100FBDBEA6B614989B210D269DBF171746A9507BB3DAE292BDAF85848A7AA091ECA02205D23A46269A904C40076C76EADFDB8F98E7D0349C0BFE5915CCA3F8835A4A41683475221034758CEFCB75E16E4DFAFB32383B709FA632086EA5CA982712DE6ADD93060B17A2103FE96237629128A0AE8C3825AF8A4BE8FE3109B16F62AF19CEC0B1EB93B8717E252AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("80BA8C010000000017A914D0C15A7D41500976056B3345F542D8C944077C8A87"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(217657L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A914D0C15A7D41500976056B3345F542D8C944077C8A87"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("004930460221008662E2BFCC4741D92531F566C5765D849E24B45C289607593584FBBA938D88CC022100D718E710D5991BDFBDC5B0D52AE8E4D9C1C1C719BCC348003C5E80524A04E16283483045022100FBDBEA6B614989B210D269DBF171746A9507BB3DAE292BDAF85848A7AA091ECA02205D23A46269A904C40076C76EADFDB8F98E7D0349C0BFE5915CCA3F8835A4A41683475221034758CEFCB75E16E4DFAFB32383B709FA632086EA5CA982712DE6ADD93060B17A2103FE96237629128A0AE8C3825AF8A4BE8FE3109B16F62AF19CEC0B1EB93B8717E252AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_AFD9C17F8913577EC3509520BD6E5D63E9C0FD2A5F70C787993B097BA6CA9FAE_0() {
        // NOTE: This transaction has multiple inputs using SIGHASH_SINGLE

        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("010000000370AC0A1AE588AAF284C308D67CA92C69A39E2DB81337E563BF40C59DA0A5CF63000000006A4730440220360D20BAFF382059040BA9BE98947FD678FB08AAB2BB0C172EFA996FD8ECE9B702201B4FB0DE67F015C90E7AC8A193AEAB486A1F587E0F54D0FB9552EF7F5CE6CAEC032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF7D815B6447E35FBEA097E00E028FB7DFBAD4F3F0987B4734676C84F3FCD0E804010000006B483045022100C714310BE1E3A9FF1C5F7CACC65C2D8E781FC3A88CEB063C6153BF950650802102200B2D0979C76E12BB480DA635F192CC8DC6F905380DD4AC1FF35A4F68F462FFFD032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF3F1F097333E4D46D51F5E77B53264DB8F7F5D2E18217E1099957D0F5AF7713EE010000006C493046022100B663499EF73273A3788DEA342717C2640AC43C5A1CF862C9E09B206FCB3F6BB8022100B09972E75972D9148F2BDD462E5CB69B57C1214B88FC55CA638676C07CFC10D8032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF0380841E00000000001976A914BFB282C70C4191F45B5A6665CAD1682F2C9CFDFB88AC80841E00000000001976A9149857CC07BED33A5CF12B9C5E0500B675D500C81188ACE0FD1C00000000001976A91443C52850606C872403C0601E69FA34B26F62DB4A88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("70AC0A1AE588AAF284C308D67CA92C69A39E2DB81337E563BF40C59DA0A5CF63000000006A4730440220360D20BAFF382059040BA9BE98947FD678FB08AAB2BB0C172EFA996FD8ECE9B702201B4FB0DE67F015C90E7AC8A193AEAB486A1F587E0F54D0FB9552EF7F5CE6CAEC032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(0, HexUtil.hexStringToByteArray("80841E00000000001976A914DCF72C4FD02F5A987CF9B02F2FABFCAC3341A87D88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(238798L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A914DCF72C4FD02F5A987CF9B02F2FABFCAC3341A87D88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("4730440220360D20BAFF382059040BA9BE98947FD678FB08AAB2BB0C172EFA996FD8ECE9B702201B4FB0DE67F015C90E7AC8A193AEAB486A1F587E0F54D0FB9552EF7F5CE6CAEC032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741A"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_AFD9C17F8913577EC3509520BD6E5D63E9C0FD2A5F70C787993B097BA6CA9FAE_1() {
        // NOTE: This transaction has multiple inputs using SIGHASH_SINGLE and is the second half of the above scenario...

        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("010000000370AC0A1AE588AAF284C308D67CA92C69A39E2DB81337E563BF40C59DA0A5CF63000000006A4730440220360D20BAFF382059040BA9BE98947FD678FB08AAB2BB0C172EFA996FD8ECE9B702201B4FB0DE67F015C90E7AC8A193AEAB486A1F587E0F54D0FB9552EF7F5CE6CAEC032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF7D815B6447E35FBEA097E00E028FB7DFBAD4F3F0987B4734676C84F3FCD0E804010000006B483045022100C714310BE1E3A9FF1C5F7CACC65C2D8E781FC3A88CEB063C6153BF950650802102200B2D0979C76E12BB480DA635F192CC8DC6F905380DD4AC1FF35A4F68F462FFFD032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF3F1F097333E4D46D51F5E77B53264DB8F7F5D2E18217E1099957D0F5AF7713EE010000006C493046022100B663499EF73273A3788DEA342717C2640AC43C5A1CF862C9E09B206FCB3F6BB8022100B09972E75972D9148F2BDD462E5CB69B57C1214B88FC55CA638676C07CFC10D8032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF0380841E00000000001976A914BFB282C70C4191F45B5A6665CAD1682F2C9CFDFB88AC80841E00000000001976A9149857CC07BED33A5CF12B9C5E0500B675D500C81188ACE0FD1C00000000001976A91443C52850606C872403C0601E69FA34B26F62DB4A88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("7D815B6447E35FBEA097E00E028FB7DFBAD4F3F0987B4734676C84F3FCD0E804010000006B483045022100C714310BE1E3A9FF1C5F7CACC65C2D8E781FC3A88CEB063C6153BF950650802102200B2D0979C76E12BB480DA635F192CC8DC6F905380DD4AC1FF35A4F68F462FFFD032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741AFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("80841E00000000001976A914DCF72C4FD02F5A987CF9B02F2FABFCAC3341A87D88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(238798L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(1);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A914DCF72C4FD02F5A987CF9B02F2FABFCAC3341A87D88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("483045022100C714310BE1E3A9FF1C5F7CACC65C2D8E781FC3A88CEB063C6153BF950650802102200B2D0979C76E12BB480DA635F192CC8DC6F905380DD4AC1FF35A4F68F462FFFD032103579CA2E6D107522F012CD00B52B9A65FB46F0C57B9B8B6E377C48F526A44741A"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_315AC7D4C26D69668129CC352851D9389B4A6868F1509C6C8B66BEAD11E2619F_1() {
        // NOTE: This transactions triggers the (infamous) bug that occurs when SIGHASH_SINGLE is used on an input that doesn't have a matching output.
        //  Original Discussion: https://bitcointalk.org/index.php?topic=260595.0

        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000002DC38E9359BD7DA3B58386204E186D9408685F427F5E513666DB735AA8A6B2169000000006A47304402205D8FEEB312478E468D0B514E63E113958D7214FA572ACD87079A7F0CC026FC5C02200FA76EA05BF243AF6D0F9177F241CAF606D01FCFD5E62D6BEFBCA24E569E5C27032102100A1A9CA2C18932D6577C58F225580184D0E08226D41959874AC963E3C1B2FEFFFFFFFFDC38E9359BD7DA3B58386204E186D9408685F427F5E513666DB735AA8A6B2169010000006B4830450220087EDE38729E6D35E4F515505018E659222031273B7366920F393EE3AB17BC1E022100CA43164B757D1A6D1235F13200D4B5F76DD8FDA4EC9FC28546B2DF5B1211E8DF03210275983913E60093B767E85597CA9397FB2F418E57F998D6AFBBC536116085B1CBFFFFFFFF0140899500000000001976A914FCC9B36D38CF55D7D5B4EE4DDDB6B2C17612F48C88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("DC38E9359BD7DA3B58386204E186D9408685F427F5E513666DB735AA8A6B2169010000006B4830450220087EDE38729E6D35E4F515505018E659222031273B7366920F393EE3AB17BC1E022100CA43164B757D1A6D1235F13200D4B5F76DD8FDA4EC9FC28546B2DF5B1211E8DF03210275983913E60093B767E85597CA9397FB2F418E57F998D6AFBBC536116085B1CBFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("F0874B00000000001976A91433CEF61749D11BA2ADF091A5E045678177FE3A6D88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(247940L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(1);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A91433CEF61749D11BA2ADF091A5E045678177FE3A6D88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("4830450220087EDE38729E6D35E4F515505018E659222031273B7366920F393EE3AB17BC1E022100CA43164B757D1A6D1235F13200D4B5F76DD8FDA4EC9FC28546B2DF5B1211E8DF03210275983913E60093B767E85597CA9397FB2F418E57F998D6AFBBC536116085B1CB"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_transaction_DA47BD83967D81F3CF6520F4FF81B3B6C4797BFE7AC2B5969AEDBF01A840CDA6_0() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000003FE1A25C8774C1E827F9EBDAE731FE609FF159D6F7C15094E1D467A99A01E03100000000002012AFFFFFFFF53A080075D834402E916390940782236B29D23DB6F52DFC940A12B3EFF99159C0000000000FFFFFFFF61E4ED95239756BBB98D11DCF973146BE0C17CC1CC94340DEB8BC4D44CD88E92000000000A516352676A675168948CFFFFFFFF0220AA4400000000001976A9149BC0BBDD3024DA4D0C38ED1AECF5C68DD1D3FA1288AC20AA4400000000001976A914169FF4804FD6596DEB974F360C21584AA1E19C9788AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("FE1A25C8774C1E827F9EBDAE731FE609FF159D6F7C15094E1D467A99A01E03100000000002012AFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(0, HexUtil.hexStringToByteArray("A0860100000000000182"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(249977L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("82"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("012A"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }
}
