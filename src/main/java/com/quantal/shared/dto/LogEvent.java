package com.quantal.shared.dto;

import com.quantal.shared.logger.LogField;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Created by dman on 09/10/2017.
 */
@Data
@AllArgsConstructor
public class LogEvent {
    private String event;
    //private String msg;
}
