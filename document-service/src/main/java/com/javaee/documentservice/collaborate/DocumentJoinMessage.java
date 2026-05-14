package com.javaee.documentservice.collaborate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentJoinMessage {

    private String documentId;

    private String userId;

    private String userName;

    private String action;
}