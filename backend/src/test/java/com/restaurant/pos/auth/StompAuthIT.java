package com.restaurant.pos.auth;

import com.restaurant.pos.IntegrationTest;
import com.restaurant.pos.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The KDS socket handshake cannot carry an Authorization header, so the STOMP CONNECT frame does.
 * Frames go straight into the real clientInboundChannel, no network client.
 */
class StompAuthIT extends IntegrationTest {

    @Autowired @Qualifier("clientInboundChannel") private MessageChannel clientInbound;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({"none,false", "garbage,false", "kitchen1,true"})
    void connect_isAcceptedOnlyWithAValidToken(String credentials, boolean accepted) {
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setSessionId("it-" + credentials);
        connect.setLeaveMutable(true); // as StompSubProtocolHandler does, so the interceptor can set the user
        switch (credentials) {
            case "none" -> { }
            case "garbage" -> connect.addNativeHeader("Authorization", "Bearer garbage");
            default -> connect.addNativeHeader("Authorization", "Bearer "
                    + jwtService.generateToken(userRepository.findByUsername(credentials).orElseThrow()));
        }
        Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders());

        if (accepted) {
            assertThatCode(() -> clientInbound.send(frame)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> clientInbound.send(frame)).isInstanceOf(MessagingException.class);
        }
    }

    @Test
    void subscribe_onASessionThatNeverAuthenticated_isRejected() {
        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setSessionId("it-skipped-connect");
        subscribe.setSubscriptionId("sub-0");
        subscribe.setDestination("/topic/orders");
        Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());

        assertThatThrownBy(() -> clientInbound.send(frame)).isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribe_onAnAuthenticatedSession_isAccepted() {
        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setSessionId("it-authenticated");
        subscribe.setSubscriptionId("sub-0");
        subscribe.setDestination("/topic/orders");
        subscribe.setUser(() -> "kitchen1"); // what StompSubProtocolHandler sets from the session after CONNECT
        Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());

        assertThatCode(() -> clientInbound.send(frame)).doesNotThrowAnyException();
    }
}
