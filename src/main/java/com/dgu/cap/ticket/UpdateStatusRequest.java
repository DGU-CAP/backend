package com.dgu.cap.ticket;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateStatusRequest {

    private String status;
    private String action;
    private String memo;
    private String performedBy;
}
