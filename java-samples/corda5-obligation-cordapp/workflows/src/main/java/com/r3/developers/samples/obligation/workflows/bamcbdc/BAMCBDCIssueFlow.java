package com.r3.developers.samples.obligation.workflows.bamcbdc;

import com.r3.developers.samples.obligation.contracts.BAMCBDCContract;
import com.r3.developers.samples.obligation.states.BAMCBDCState;
import com.r3.developers.samples.obligation.workflows.FinalizeIOUFlow;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.FlowEngine;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.common.Party;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class BAMCBDCIssueFlow implements ClientStartableFlow {
    private final static Logger log = LoggerFactory.getLogger(BAMCBDCIssueFlow.class);

    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the MemberLookup to look up the VNode identities.
    @CordaInject
    public MemberLookup memberLookup;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService ledgerService;

    // Injects the NotaryLookup to look up the notary identity.
    @CordaInject
    public NotaryLookup notaryLookup;

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;

    @Override
    @Suspendable
    public String call(ClientRequestBody requestBody) {
        log.info("BAMCBDCIssueFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            BAMCBDCIssueFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, BAMCBDCIssueFlowArgs.class);

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo receiverInfo = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getReceiver())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            // Create the IOUState from the input arguments and member information.

            List<StateAndRef<BAMCBDCState>> states = ledgerService.findUnconsumedStatesByType(BAMCBDCState.class);
            List<ListBAMCBDCFlowResults> results = states.stream().map(stateAndRef ->
                    new ListBAMCBDCFlowResults(
                            stateAndRef.getState().getContractState().getLinearId(),
                            stateAndRef.getState().getContractState().getAmount(),
                            stateAndRef.getState().getContractState().getReceiver().toString(),
                            stateAndRef.getState().getContractState().getSender().toString(),
                            stateAndRef.getState().getContractState().getSolde()
                    )
            ).collect(Collectors.toList());
            int listSize=results.size()-1;
            float solde=0;
            if(listSize>0)
                solde=results.get(listSize).getSolde();
            BAMCBDCState bamcbdcstate = new BAMCBDCState(Float.parseFloat(flowArgs.getAmount()),myInfo.getName(),
                    receiverInfo.getName(),solde+Float.parseFloat(flowArgs.getAmount()), UUID.randomUUID(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), receiverInfo.getLedgerKeys().get(0)));

            // Obtain the Notary name and public key.
            NotaryInfo notary = requireNonNull(
                    notaryLookup.lookup(MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")),
                    "NotaryLookup can't find notary specified in flow arguments."
            );


            PublicKey notaryKey = null;
            for(MemberInfo memberInfo: memberLookup.lookup()){
                if(Objects.equals(
                        memberInfo.getMemberProvidedContext().get("corda.notary.service.name"),
                        notary.getName().toString())) {
                    notaryKey = memberInfo.getLedgerKeys().get(0);
                    break;
                }
            }

            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            if(notaryKey == null) {
                throw new CordaRuntimeException("No notary PublicKey found");
            }

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.getTransactionBuilder()
                    .setNotary(new Party(notary.getName(), notaryKey))
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(bamcbdcstate)
                    .addCommand(new BAMCBDCContract.Issue())
                    .addSignatories(bamcbdcstate.getParticipants());

            // Convert the transaction builder to a UTXOSignedTransaction and sign with this Vnode's first Ledger key.
            // Note, toSignedTransaction() is currently a placeholder method, hence being marked as deprecated.
            @SuppressWarnings("DEPRECATION")
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction();

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(new FinalizeBAMCBDCFlow.FinalizeBAMCBDC(signedTransaction, Arrays.asList(receiverInfo.getName())));
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "createiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUIssueFlow",
    "requestBody": {
        "amount":"20",
        "lender":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */
