package com.frwss.system.messaging.controller;

import com.frwss.system.messaging.dto.NotificationDto;
import com.frwss.system.messaging.model.Notification;
import com.frwss.system.messaging.service.NotificationService;
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

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SecurityContextAdapter securityContextAdapter;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository,
            SecurityContextAdapter securityContextAdapter) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.securityContextAdapter = securityContextAdapter;
    }

    private Long getCurrentUserId() {
        try {
            String username = securityContextAdapter.getCurrentUsername();
            if (username == null || username.isEmpty()) {
                log.warn("Username is null or empty");
                return null;
            }
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user == null) {
                log.warn("User not found: {}", username);
                return null;
            }
            return user.getUserId().longValue();
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage());
            return null;
        }
    }

    @GetMapping
    public String notifications(Model model) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                log.warn("User not authenticated for notifications page");
                model.addAttribute("error", "User not authenticated");
                model.addAttribute("notifications", List.of());
                model.addAttribute("unreadCount", 0);
                return "notifications/notifications";
            }

            List<Notification> notifications = notificationService.getNotifications(userId);
            long unreadCount = notificationService.getUnreadCount(userId);

            log.info("Loaded {} notifications for user {}, {} unread",
                    notifications != null ? notifications.size() : 0,
                    userId, unreadCount);

            model.addAttribute("notifications", notifications != null ? notifications : List.of());
            model.addAttribute("unreadCount", unreadCount);
        } catch (Exception e) {
            log.error("Error loading notifications: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load notifications: " + e.getMessage());
            model.addAttribute("notifications", List.of());
            model.addAttribute("unreadCount", 0);
        }
        return "notifications/notifications";
    }

    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                redirectAttributes.addFlashAttribute("error", "User not authenticated");
                return "redirect:/notifications";
            }
            notificationService.markAsRead(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Notification marked as read!");
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to mark notification as read: " + e.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                redirectAttributes.addFlashAttribute("error", "User not authenticated");
                return "redirect:/notifications";
            }
            notificationService.markAllAsRead(userId);
            redirectAttributes.addFlashAttribute("successMessage", "✅ All notifications marked as read!");
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to mark notifications as read: " + e.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                redirectAttributes.addFlashAttribute("error", "User not authenticated");
                return "redirect:/notifications";
            }
            notificationService.deleteNotification(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "🗑️ Notification deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting notification: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to delete notification: " + e.getMessage());
        }
        return "redirect:/notifications";
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public long getUnreadCount() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) return 0;
            return notificationService.getUnreadCount(userId);
        } catch (Exception e) {
            log.error("Error getting unread count: {}", e.getMessage());
            return 0;
        }
    }

    @GetMapping("/api/latest")
    @ResponseBody
    public List<NotificationDto> getLatestNotifications() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) return List.of();

            List<Notification> notifications = notificationService.getNotifications(userId);
            if (notifications == null) return List.of();

            return notifications.stream()
                    .limit(5)
                    .map(n -> {
                        NotificationDto dto = new NotificationDto();
                        dto.setId(n.getId());
                        dto.setTitle(n.getTitle());
                        dto.setMessage(n.getMessage());
                        dto.setType(n.getType());
                        dto.setIsRead(n.getIsRead());
                        dto.setLink(n.getLink());
                        dto.setCreatedAt(n.getCreatedAt());
                        dto.setEntityType(n.getEntityType());
                        dto.setEntityId(n.getEntityId());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting latest notifications: {}", e.getMessage());
            return List.of();
        }
    }
}