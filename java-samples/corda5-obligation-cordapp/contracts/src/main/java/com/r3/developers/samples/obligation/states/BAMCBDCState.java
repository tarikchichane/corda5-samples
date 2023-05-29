package com.r3.developers.samples.obligation.states;

import com.r3.developers.samples.obligation.contracts.BAMCBDCContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

@BelongsToContract(BAMCBDCContract.class)
public class BAMCBDCState implements ContractState {
    public  float amount;
    public  MemberX500Name sender;
    public  MemberX500Name receiver;
    public float solde;
    private  UUID linearId;

    @ConstructorForDeserialization
     public BAMCBDCState(float amount, MemberX500Name sender, MemberX500Name receiver, float solde, UUID linearId, List<PublicKey> participants) {
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.solde = solde;
        this.linearId = linearId;
        this.participants = participants;
    }
    public BAMCBDCState(float amount, MemberX500Name sender, MemberX500Name receiver, float solde, UUID linearId) {
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.solde = solde;
        this.linearId = linearId;
    }

    public BAMCBDCState(float amount, MemberX500Name sender, MemberX500Name receiver,  List<PublicKey> participants) {
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.participants=participants;
    }

    public BAMCBDCState( MemberX500Name sender, MemberX500Name receiver, float solde, UUID linearId){
        this.sender=sender;
        this.receiver=receiver;
        this.solde=solde;
        this.linearId=linearId;
    }
    public float getAmount() {
        return amount;
    }

    public List<PublicKey> participants;


    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

    public MemberX500Name getSender() {
        return sender;
    }

    public MemberX500Name getReceiver() {
        return receiver;
    }

    public float getSolde() {
        return solde;
    }

    public UUID getLinearId() {
        return linearId;
    }
}
