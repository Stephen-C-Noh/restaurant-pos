package com.restaurant.pos.auth;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** Authenticates the STOMP CONNECT frame and rejects SUBSCRIBE without a user: the SockJS handshake cannot carry an Authorization header. */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Missing bearer token");
            }
            accessor.setUser(jwtService.authenticate(authHeader.substring(7))
                    .orElseThrow(() -> new MessagingException("Invalid token")));
        }
        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && accessor.getUser() == null) {
            throw new MessagingException("Subscribe needs an authenticated session");
        }
        return message;
    }
}
