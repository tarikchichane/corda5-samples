package com.r3.developers.samples.obligation.contracts;

import com.r3.developers.samples.obligation.states.BAMCBDCState;
import com.r3.developers.samples.obligation.states.IOUState;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.ContractState;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;

import java.security.PublicKey;
import java.util.Set;

public class BAMCBDCContract implements Contract {
    @Override
    public boolean isRelevant(ContractState state, Set<PublicKey> myKeys) {
        return Contract.super.isRelevant(state, myKeys);
    }
    public static class Issue implements Command { }

    public static class Transfer implements Command { }
    @Override
    public void verify(UtxoLedgerTransaction transaction) {

        // Ensures that there is only one command in the transaction
        requireThat( transaction.getCommands().size() == 1, "Require a single command.");
        Command command = transaction.getCommands().get(0);
        BAMCBDCState output = transaction.getOutputStates(BAMCBDCState.class).get(0);
        requireThat(output.getParticipants().size() == 2, "The output state should have two and only two participants.");
        if(command.getClass() == BAMCBDCContract.Issue.class) {// Rules applied only to transactions with the Issue Command.
            requireThat(transaction.getOutputContractStates().size() == 1, "Only one output states should be created when issuing an BAMCBDC.");
        }else if(command.getClass() == BAMCBDCContract.Transfer.class) {// Rules applied only to transactions with the Transfer Command.
            requireThat( transaction.getInputContractStates().size() > 0, "There must be one input BAMCBDC.");
        }
        else {
            throw new CordaRuntimeException("Unsupported command");
        }
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
