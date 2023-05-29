package com.r3.developers.samples.obligation.workflows.bamcbdc;

// A class to hold the deserialized arguments required to start the flow.
public class BAMCBDCIssueFlowArgs {
    private  String amount;
    private  String receiver;

    public BAMCBDCIssueFlowArgs() {
    }

    public BAMCBDCIssueFlowArgs(String amount, String receiver) {
        this.amount = amount;
        this.receiver = receiver;
    }

    public String getAmount() {
        return amount;
    }

    public String getReceiver() {
        return receiver;
    }
}
