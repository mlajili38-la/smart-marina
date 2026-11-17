package tn.iset.marina.notification_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.marina.notification_service.dto.NotificationPageResponse;
import tn.iset.marina.notification_service.dto.NotificationResponse;
import tn.iset.marina.notification_service.security.CurrentUser;
import tn.iset.marina.notification_service.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @Operation(summary = "Lister les notifications (filtres portId, unreadOnly)")
    public NotificationPageResponse getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) Long portId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String userPortId) {
        return service.findAll(pageable, portId, unreadOnly, CurrentUser.of(userId, roles, userPortId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String userPortId) {
        return service.markAsRead(id, CurrentUser.of(userId, roles, userPortId));
    }
}
