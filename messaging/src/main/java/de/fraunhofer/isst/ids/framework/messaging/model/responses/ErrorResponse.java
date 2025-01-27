package de.fraunhofer.isst.ids.framework.messaging.model.responses;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of MessageResponse used for returning RejectionMessages and Error descriptions.
 */
@Data
@Slf4j
public class ErrorResponse implements MessageResponse {

    private final RejectionMessage rejectionMessage;
    private final String errorMessage;

    /**
     * Create an ErrorResponse with a RejectionMessage header and errorReason String payload
     *
     * @param rejectionMessage a RejectionMessage
     * @param errorMessage a detailed Error description
     */
    public ErrorResponse(RejectionMessage rejectionMessage, String errorMessage){
        this.rejectionMessage = rejectionMessage;
        this.errorMessage = errorMessage;
    }

    /**
     * Create an ErrorResponse with a RejectionMessage header and errorReason String payload
     *
     * @param rejectionMessage a RejectionMessage
     * @param errorReason a detailed Error description
     * @return an instance of ErrorResponse with the given parameters
     */
    public static ErrorResponse create(final RejectionMessage rejectionMessage, final String errorReason) {
        return new ErrorResponse(rejectionMessage, errorReason);
    }

    /**
     * Create an ErrorResponse with Default RejectionMessage as header (only RejectionReason has to be Provided)
     *
     * @param rejectionReason RejectionReason (why the message was rejected)
     * @param errorMessage detailed error description
     * @param connectorId id of the current connector
     * @param modelVersion infomodelversion of the current connector
     * @param messageId id of the message being rejected (from message-header)
     * @return an instance of ErrorResponse with the given parameters
     */
    public static ErrorResponse withDefaultHeader(final RejectionReason rejectionReason, final String errorMessage, final URI connectorId, final String modelVersion, URI messageId){
        if(messageId == null) {
            messageId = URI.create("https://INVALID");
        }

        var rejectionMessage = new RejectionMessageBuilder()
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("rejected!").build())
                ._correlationMessage_(messageId)
                ._senderAgent_(connectorId)
                ._issuerConnector_(connectorId)
                ._modelVersion_(modelVersion)
                ._rejectionReason_(rejectionReason)
                ._issued_(IDSUtils.getGregorianNow())
                .build();
        return new ErrorResponse(rejectionMessage, errorMessage);
    }

    /**
     * Create an ErrorResponse with Default RejectionMessage as header (only RejectionReason has to be Provided)
     *
     * @param rejectionReason RejectionReason (why the message was rejected)
     * @param errorMessage detailed error description
     * @param connectorId id of the current connector
     * @param modelVersion infomodelversion of the current connector
     * @return an instance of ErrorResponse with the given parameters
     */
    public static ErrorResponse withDefaultHeader(final RejectionReason rejectionReason, final String errorMessage, final URI connectorId, final String modelVersion){
        return withDefaultHeader(rejectionReason, errorMessage, connectorId, modelVersion, null);
    }

    /**{@inheritDoc}*/
    @Override
    public Map<String, Object> createMultipartMap(Serializer serializer) throws IOException {
        var multiMap = new LinkedHashMap<String, Object>();
        multiMap.put("header", serializer.serialize(rejectionMessage));
        multiMap.put("payload", errorMessage);
        return multiMap;
    }
}
