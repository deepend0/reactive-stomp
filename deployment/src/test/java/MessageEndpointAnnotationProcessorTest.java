import com.github.deepend0.reactivestomp.messageendpoint.MessageEndpointMethodWrapper;
import com.github.deepend0.reactivestomp.messageendpoint.MessageEndpointRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MessageEndpointAnnotationProcessorTest {
    @RegisterExtension
    static final QuarkusUnitTest quarkusUnitTest  = new QuarkusUnitTest().withEmptyApplication();

    @Inject
    private MessageEndpointRegistry messageEndpointRegistry;

    @Test
    public void shouldProcessSampleEndpoint() {
        List<MessageEndpointMethodWrapper<?,?>> messageEndpointMethodWrappers = messageEndpointRegistry.getMessageEndpoints("inboundDestination");
        Assertions.assertEquals(1, messageEndpointMethodWrappers.size());
        MessageEndpointMethodWrapper<String, String> messageEndpointMethodWrapper = (MessageEndpointMethodWrapper<String, String>) messageEndpointMethodWrappers.getFirst();
        Assertions.assertEquals("inboundDestination", messageEndpointMethodWrapper.getMessageEndpoint().inboundDestination());
        Assertions.assertEquals("outboundDestination", messageEndpointMethodWrapper.getMessageEndpoint().inboundDestination());
        Assertions.assertEquals(String.class, messageEndpointMethodWrapper.getParameterType());
        List<String> result = new ArrayList<>();
        Uni<String> uniResult = (Uni<String>) messageEndpointMethodWrapper.getMethodWrapper().apply("World");
        uniResult.subscribe().with(result::add);
        Awaitility.await().atMost(Duration.ofMillis(3000)).pollInterval(Duration.ofMillis(1000)).until(()->"Hello World".equals(result.getFirst()));
    }
}
