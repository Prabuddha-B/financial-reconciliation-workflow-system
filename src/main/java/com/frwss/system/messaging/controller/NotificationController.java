package com.frwss.system.messaging.controller;

import com.frwss.system.messaging.dto.NotificationDto;
import com.frwss.system.messaging.model.Notification;
import com.frwss.system.messaging.service.NotificationService;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for notification functionality
 *
 * URL Mapping:
 * - GET  /notifications         - View all notifications
 * - POST /notifications/mark-read/{id} - Mark as read
 * - POST /notifications/mark-all-read  - Mark all as read
 * - POST /notifications/delete/{id}    - Delete a notification
 * - GET  /notifications/api/unread-count - API endpoint for badge
 */

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SecurityContextAdapter securityContextAdapter;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository,
            SecurityContextAdapter securityContextAdapter){
        this.notificationService = notificationService;
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
     * Display all notifications for the current user
     */
    @GetMapping
    public String notifications(Model model) {
        Long userId = getCurrentUserId();
        List<Notification> notifications = notificationService.getNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications/notifications";
    }

    /**
     * Mark a notification as read
     */
    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            notificationService.markAsRead(id, userId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Notification marked as read!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to mark notification as read: " + e.getMessage());
        }

        return "redirect:/notifications";
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            notificationService.markAllAsRead(userId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "All notifications marked as read!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to mark notifications as read: " + e.getMessage());
        }

        return "redirect:/notifications";
    }

    /**
     * Delete a notification
     */
    @PostMapping("/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            notificationService.deleteNotification(id, userId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Notification deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete notification: " + e.getMessage());
        }

        return "redirect:/notifications";
    }

    /**
     * API endpoint for unread count (for AJAX polling)
     * Used by the navbar to show notification badge
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public long getUnreadCount() {
        try {
            Long userId = getCurrentUserId();
            return notificationService.getUnreadCount(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/api/latest")
    @ResponseBody
    public List<NotificationDto> getLatestNotifications() {
        Long userId = getCurrentUserId();
        List<Notification> notifications = notificationService.getNotifications(userId);

        // Return only the 5 most recent notifications
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
                    return dto;
                })
                .collect(Collectors.toList());
    }


}
