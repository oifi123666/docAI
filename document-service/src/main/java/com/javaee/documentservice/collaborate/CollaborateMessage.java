package com.javaee.documentservice.collaborate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollaborateMessage {

    private String type;

    private EditOperation editOperation;

    private CursorPosition cursorPosition;

    private DocumentJoinMessage joinMessage;

    public static CollaborateMessage edit(EditOperation op) {
        CollaborateMessage msg = new CollaborateMessage();
        msg.setType("EDIT");
        msg.setEditOperation(op);
        return msg;
    }

    public static CollaborateMessage cursor(CursorPosition cursor) {
        CollaborateMessage msg = new CollaborateMessage();
        msg.setType("CURSOR");
        msg.setCursorPosition(cursor);
        return msg;
    }

    public static CollaborateMessage join(DocumentJoinMessage join) {
        CollaborateMessage msg = new CollaborateMessage();
        msg.setType("JOIN");
        msg.setJoinMessage(join);
        return msg;
    }

    public static CollaborateMessage leave(DocumentJoinMessage leave) {
        CollaborateMessage msg = new CollaborateMessage();
        msg.setType("LEAVE");
        msg.setJoinMessage(leave);
        return msg;
    }
}