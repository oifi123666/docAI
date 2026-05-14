package com.javaee.documentservice.controller;

import com.javaee.common.utils.JwtUtils;
import com.javaee.documentservice.collaborate.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class CollaborateController {

    private static final Logger log = LoggerFactory.getLogger(CollaborateController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CollaborateSessionManager sessionManager;

    @Autowired
    private EditOperationHandler editOperationHandler;

    @Autowired
    private CursorSyncHandler cursorSyncHandler;

    @Autowired
    private DocumentSnapshotService snapshotService;

    @MessageMapping("/collaborate/join")
    public void joinDocument(@Payload DocumentJoinMessage joinMessage, Principal principal) {
        String userId = extractUserId(principal);
        String userName = extractUserName(principal);

        joinMessage.setUserId(userId);
        joinMessage.setUserName(userName);

        sessionManager.userJoinDocument(joinMessage.getDocumentId(), userId, userName);

        CollaborateMessage joinMsg = CollaborateMessage.join(joinMessage);
        messagingTemplate.convertAndSend("/topic/doc/" + joinMessage.getDocumentId(), joinMsg);

        List<CursorPosition> cursors = cursorSyncHandler.getDocumentCursors(joinMessage.getDocumentId());
        for (CursorPosition cursor : cursors) {
            if (!cursor.getUserId().equals(userId)) {
                messagingTemplate.convertAndSend("/topic/doc/" + joinMessage.getDocumentId(),
                        CollaborateMessage.cursor(cursor));
            }
        }

        log.info("用户加入协同编辑: userId={}, userName={}, documentId={}",
                userId, userName, joinMessage.getDocumentId());
    }

    @MessageMapping("/collaborate/leave")
    public void leaveDocument(@Payload DocumentJoinMessage leaveMessage, Principal principal) {
        String userId = extractUserId(principal);
        String userName = extractUserName(principal);

        leaveMessage.setUserId(userId);
        leaveMessage.setUserName(userName);

        sessionManager.userLeaveDocument(leaveMessage.getDocumentId(), userId);
        cursorSyncHandler.removeCursor(leaveMessage.getDocumentId(), userId);

        CollaborateMessage leaveMsg = CollaborateMessage.leave(leaveMessage);
        messagingTemplate.convertAndSend("/topic/doc/" + leaveMessage.getDocumentId(), leaveMsg);

        log.info("用户离开协同编辑: userId={}, documentId={}", userId, leaveMessage.getDocumentId());
    }

    @MessageMapping("/collaborate/edit")
    public void handleEdit(@Payload EditOperation operation, Principal principal) {
        String userId = extractUserId(principal);
        String userName = extractUserName(principal);

        operation.setUserId(userId);
        operation.setUserName(userName);

        long clock = editOperationHandler.getNextClock(operation.getDocumentId(), userId);
        operation.setClock(clock);
        operation.setOpId(userId + "-" + clock);

        editOperationHandler.saveOperation(operation);

        CollaborateMessage editMsg = CollaborateMessage.edit(operation);
        messagingTemplate.convertAndSend("/topic/doc/" + operation.getDocumentId(), editMsg);

        log.debug("编辑操作已广播: opId={}, type={}, documentId={}, userId={}",
                operation.getOpId(), operation.getType(), operation.getDocumentId(), userId);
    }

    @MessageMapping("/collaborate/cursor")
    public void handleCursor(@Payload CursorPosition cursor, Principal principal) {
        String userId = extractUserId(principal);
        String userName = extractUserName(principal);

        cursor.setUserId(userId);
        cursor.setUserName(userName);
        cursor.setTimestamp(System.currentTimeMillis());

        cursorSyncHandler.updateCursor(cursor);

        CollaborateMessage cursorMsg = CollaborateMessage.cursor(cursor);
        messagingTemplate.convertAndSend("/topic/doc/" + cursor.getDocumentId(), cursorMsg);
    }

    @MessageMapping("/collaborate/sync")
    public void handleSync(@Payload Map<String, Object> syncRequest, Principal principal) {
        String userId = extractUserId(principal);
        String documentId = (String) syncRequest.get("documentId");
        long sinceClock = syncRequest.get("sinceClock") != null
                ? ((Number) syncRequest.get("sinceClock")).longValue() : 0;

        List<EditOperation> missedOps = editOperationHandler.getOperationsSince(documentId, sinceClock);
        for (EditOperation op : missedOps) {
            CollaborateMessage editMsg = CollaborateMessage.edit(op);
            messagingTemplate.convertAndSendToUser(userId, "/topic/doc/" + documentId, editMsg);
        }

        log.debug("同步操作: userId={}, documentId={}, sinceClock={}, missedOps={}",
                userId, documentId, sinceClock, missedOps.size());
    }

    private String extractUserId(Principal principal) {
        if (principal != null) {
            return principal.getName();
        }
        return "anonymous";
    }

    private String extractUserName(Principal principal) {
        if (principal != null) {
            return principal.getName();
        }
        return "anonymous";
    }
}