package com.javaee.documentservice.collaborate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditOperation {

    private String opId;

    private String documentId;

    private String userId;

    private String userName;

    private OpType type;

    private int from;

    private int to;

    private String text;

    private long clock;

    public enum OpType {
        INSERT,
        DELETE
    }

    public static EditOperation createInsert(String documentId, String userId, String userName,
                                              int position, String text, long clock) {
        EditOperation op = new EditOperation();
        op.setOpId(userId + "-" + clock);
        op.setDocumentId(documentId);
        op.setUserId(userId);
        op.setUserName(userName);
        op.setType(OpType.INSERT);
        op.setFrom(position);
        op.setTo(position);
        op.setText(text);
        op.setClock(clock);
        return op;
    }

    public static EditOperation createDelete(String documentId, String userId, String userName,
                                              int from, int to, long clock) {
        EditOperation op = new EditOperation();
        op.setOpId(userId + "-" + clock);
        op.setDocumentId(documentId);
        op.setUserId(userId);
        op.setUserName(userName);
        op.setType(OpType.DELETE);
        op.setFrom(from);
        op.setTo(to);
        op.setText("");
        op.setClock(clock);
        return op;
    }
}