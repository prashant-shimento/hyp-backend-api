package com.hyp.controller;

import com.hyp.entity.Broadcast;
import com.hyp.enums.BroadcastScope;
import com.hyp.request.BroadcastRequest;
import com.hyp.service.BroadcastService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/broadcast")
public class BroadcastController {

    @Autowired
    private BroadcastService broadcastService;

    /** Send an announcement or mode-change. */
    @PostMapping
    public ResponseEntity<Broadcast> send(@Valid @RequestBody BroadcastRequest request) {
        return ResponseEntity.ok(broadcastService.send(request));
    }

    /** Explicitly clear an active announcement. */
    @DeleteMapping("/{broadcastId}")
    public ResponseEntity<Void> clear(@PathVariable String broadcastId) {
        broadcastService.clear(broadcastId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get currently active announcements for a scope + target.
     * Clients call this on connect to hydrate current state.
     * Mode state is read from GET /restaurant/{id} directly.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Broadcast>> getActive(
            @RequestParam BroadcastScope scope, @RequestParam String targetId) {
        return ResponseEntity.ok(broadcastService.getActive(scope, targetId));
    }
}
