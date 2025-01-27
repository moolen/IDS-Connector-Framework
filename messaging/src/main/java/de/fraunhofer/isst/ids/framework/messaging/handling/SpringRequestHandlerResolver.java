package de.fraunhofer.isst.ids.framework.messaging.handling;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.MessageHandler;
import de.fraunhofer.isst.ids.framework.messaging.model.messages.SupportedMessageType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Resolver that uses the Spring dependency injection mechanism to find the matching message handler
 */
@Service
public class SpringRequestHandlerResolver implements RequestHandlerResolver {

    @Data
    @RequiredArgsConstructor
    private static class Tuple<K, V> {
        final K key;
        final V value;
    }

    private final ApplicationContext appContext;

    /**
     * Default constructor autowired by Spring and sets ApplicationContext from Spring.
     *
     * @param appContext context to access Spring CDI
     */
    @Autowired
    public SpringRequestHandlerResolver(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Resolve a MessageHandler instance that is able to handle the given messageType parameter.
     *
     * @param messageType type of the message to handle
     * @param <R> generic constraint to get a subtype of RequestMessage
     * @return optionally found matching handler instance
     */
    @SuppressWarnings("unchecked")
    public <R extends Message> Optional<MessageHandler<R>> resolveHandler(final Class<R> messageType) {
        //TODO: check if still one handler can support multiple message tpyes (SupportedMessageType)
        return Arrays.stream(appContext.getBeanNamesForAnnotation(SupportedMessageType.class))
                .flatMap(s -> Optional.ofNullable(appContext.findAnnotationOnBean(s, SupportedMessageType.class)).stream().map(msg -> new Tuple<>(s, msg)))
                .filter(t -> t.value.value().equals(messageType))
                .<MessageHandler<R>>map(t -> appContext.getBean(t.key, MessageHandler.class))
                .findFirst();
    }

}
