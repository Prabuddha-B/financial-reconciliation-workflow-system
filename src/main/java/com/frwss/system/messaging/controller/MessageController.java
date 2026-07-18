package com.frwss.system.messaging.controller;

import com.frwss.system.messaging.dto.MessageDto;
import com.frwss.system.messaging.model.Message;
import com.frwss.system.messaging.service.MessageService;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for messaging functionality
 *
 * URL Mapping:
 * - GET  /messages/inbox      - View inbox
 * - GET  /messages/sent       - View sent messages
 * - GET  /messages/compose    - Compose new message
 * - POST /messages/send       - Send message
 * - GET  /messages/view/{id}  - View a single message
 * - GET  /messages/reply/{id} - Reply to a message
 * - POST /messages/delete/{id}- Delete a message
 */

@Controller
@RequestMapping("/messages")
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SecurityContextAdapter securityContextAdapter;

    public MessageController(
            MessageService messageService,
            UserRepository userRepository,
            SecurityContextAdapter securityContextAdapter){
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.securityContextAdapter = securityContextAdapter;
    }

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        String username = securityContextAdapter.getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserId().longValue();
    }

    /**
     * Display inbox with all received messages
     */
    @GetMapping("/inbox")
    public String inbox(Model model) {
        Long userId = getCurrentUserId();
        List<Message> messages = messageService.getInbox(userId);
        long unreadCount = messageService.getUnreadCount(userId);

        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("activeTab", "inbox");

        return "messages/inbox";
    }

    /**
     * Display sent messages
     */
    @GetMapping("/sent")
    public String sent(Model model) {
        Long userId = getCurrentUserId();
        List<Message> messages = messageService.getSentMessages(userId);

        model.addAttribute("messages", messages);
        model.addAttribute("activeTab", "sent");

        return "messages/inbox";
    }

    /**
     * Display compose message page with user list
     */
    @GetMapping("/compose")
    public String compose(Model model) {
        Long userId = getCurrentUserId();

        // Get all users except current user for recipient selection
        List<User> users = userRepository.findAll();
        users.removeIf(u -> u.getUserId().equals(userId));

        model.addAttribute("users", users);
        model.addAttribute("message", new MessageDto());

        return "messages/compose";
    }

    /**
     * Send a message
     */
    @PostMapping("/send")
    public String sendMessage(
            @ModelAttribute MessageDto messageDto,
            RedirectAttributes redirectAttributes){
        try{
            Long senderId = getCurrentUserId();

            Message message = messageService.sendMessage(
                    senderId,
                    messageDto.getRecipientId(),
                    messageDto.getSubject(),
                    messageDto.getContent(),
                    messageDto.getReferenceType(),
                    messageDto.getReferenceId()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Message sent successfully!");

            return "redirect:/messages/inbox";

        } catch (Exception e){
            log.error("Failed to send message: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to send message: " + e.getMessage());
            return "redirect:/messages/compose";
        }
    }

    /**
     * View a single message
     */
    @GetMapping("/view/{id}")
    public String viewMessage(@PathVariable Long id, Model model) {
        Long userId = getCurrentUserId();
        Message message = messageService.getMessage(id);

        // Mark as read if user is the recipient
        if (message.getRecipientId().equals(userId) && !message.getIsRead()) {
            messageService.markAsRead(id, userId);
        }

        model.addAttribute("message", message);

        return "messages/view";
    }

    /**
     * Reply to a message (shows reply form)
     */
    @GetMapping("/reply/{id}")
    public String reply(@PathVariable Long id, Model model){
        Long userId = getCurrentUserId();
        Message originalMessage = messageService.getMessage(id);

        // Verify user is authorized to reply
        if (!originalMessage.getRecipientId().equals(userId) &&
                !originalMessage.getSenderId().equals(userId)) {
            throw new RuntimeException("You are not authorized to reply to this message");
        }

        MessageDto replyDto = new MessageDto();
        replyDto.setSubject("Re: " + originalMessage.getSubject());
        replyDto.setRecipientId(originalMessage.getSenderId());

        model.addAttribute("originalMessage", originalMessage);
        model.addAttribute("reply", replyDto);

        return "messages/reply";
    }

    /**
     * Send a reply
     */
    @PostMapping("/reply/send")
    public String sendReply(
            @RequestParam Long parentMessageId,
            @RequestParam String content,
            RedirectAttributes redirectAttributes){
        try {
            Long senderId = getCurrentUserId();
            Message reply = messageService.replyToMessage(parentMessageId, senderId, content);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Reply sent successfully!");

            return "redirect:/messages/view/" + reply.getId();

        } catch (Exception e) {
            log.error("Failed to send reply: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to send reply: " + e.getMessage());
            return "redirect:/messages/inbox";
        }
    }

    /**
     * Delete a message
     */
    @PostMapping("/delete/{id}")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            messageService.deleteMessage(id, userId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Message deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete message: " + e.getMessage());
        }

        return "redirect:/messages/inbox";
    }

    /**
     * Mark all messages as read
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            messageService.markAllAsRead(userId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "All messages marked as read!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to mark messages as read: " + e.getMessage());
        }

        return "redirect:/messages/inbox";
    }


    /**
     * API endpoint for unread count (for AJAX polling)
     * Used by the navbar to show message count badge
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public long getUnreadCount() {
        try {
            Long userId = getCurrentUserId();
            return messageService.getUnreadCount(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/api/latest")
    @ResponseBody
    public List<MessageDto> getLatestMessages() {
        Long userId = getCurrentUserId();
        List<Message> messages = messageService.getInbox(userId);

        // Return only the 5 most recent messages
        return messages.stream()
                .limit(5)
                .map(m -> {
                    MessageDto dto = new MessageDto();
                    dto.setId(m.getId());
                    dto.setSubject(m.getSubject());
                    dto.setContent(m.getContent());
                    dto.setSenderId(m.getSenderId());
                    dto.setRecipientId(m.getRecipientId());
                    dto.setIsRead(m.getIsRead());
                    dto.setSentAt(m.getSentAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
