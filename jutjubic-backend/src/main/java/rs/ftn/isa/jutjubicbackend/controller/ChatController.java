package rs.ftn.isa.jutjubicbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import rs.ftn.isa.jutjubicbackend.dto.ChatMessageDTO;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{videoId}/send")
    public void sendMessage(@DestinationVariable Long videoId,
                            @Payload ChatMessageDTO message,
                            SimpMessageHeaderAccessor headerAccessor) {

        // Set server-side values
        message.setVideoId(videoId);
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessageDTO.MessageType.CHAT);

        // Broadcast message to all subscribers of this video's chat
        messagingTemplate.convertAndSend("/topic/chat/" + videoId, message);
    }

    @MessageMapping("/chat/{videoId}/join")
    public void addUser(@DestinationVariable Long videoId,
                        @Payload ChatMessageDTO message,
                        SimpMessageHeaderAccessor headerAccessor) {

        // Add username in web socket session
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("username", message.getUsername());
        }

        message.setVideoId(videoId);
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessageDTO.MessageType.JOIN);

        messagingTemplate.convertAndSend("/topic/chat/" + videoId, message);
    }

    @MessageMapping("/chat/{videoId}/leave")
    public void removeUser(@DestinationVariable Long videoId,
                           @Payload ChatMessageDTO message,
                           SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String username = null;
        if (sessionAttributes != null) {
            username = (String) sessionAttributes.get("username");
        }

        if (username != null) {
            message.setVideoId(videoId);
            message.setUsername(username);
            message.setTimestamp(LocalDateTime.now());
            message.setType(ChatMessageDTO.MessageType.LEAVE);

            messagingTemplate.convertAndSend("/topic/chat/" + videoId, message);
        }
    }
}


