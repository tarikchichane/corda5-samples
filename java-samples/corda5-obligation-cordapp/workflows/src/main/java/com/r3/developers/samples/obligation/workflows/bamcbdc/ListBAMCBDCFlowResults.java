package com.r3.developers.samples.obligation.workflows.bamcbdc;

import java.util.UUID;

// A class to hold the deserialized arguments required to start the flow.
public class ListBAMCBDCFlowResults {

    private UUID id;
    private float amount;
    private String sender;
    private String receiver;
    private float solde;

    public ListBAMCBDCFlowResults() {
    }

    public ListBAMCBDCFlowResults(UUID id, float amount, String sender, String receiver, float solde) {
        this.id = id;
        this.amount = amount;
        this.sender = sender;
        this.receiver = receiver;
        this.solde = solde;
    }

    public UUID getId() {
        return id;
    }

    public float getAmount() {
        return amount;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public float getSolde() {
        return solde;
    }
}
