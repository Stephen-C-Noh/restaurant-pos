package com.restaurant.pos.auth;

import com.restaurant.pos.user.User;
import com.restaurant.pos.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Authenticates the STOMP CONNECT frame: the SockJS handshake cannot carry an Authorization header. */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public StompAuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Missing bearer token");
            }
            String token = authHeader.substring(7);
            if (!jwtService.isValid(token)) {
                throw new MessagingException("Invalid token");
            }
            Claims claims = jwtService.parseToken(token);
            User user = userRepository.findById(UUID.fromString(claims.getSubject())).orElse(null);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                throw new MessagingException("Unknown or inactive user");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
        }
        return message;
    }
}
