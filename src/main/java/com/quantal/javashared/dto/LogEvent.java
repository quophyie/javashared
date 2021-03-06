package com.quantal.javashared.dto;

import com.quantal.javashared.constants.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Created by dman on 09/10/2017.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class LogEvent extends LogField{
    private String event;
    //private String msg;

    public LogEvent(String event){
        this();
        super.setValue(event);
        this.event = event;
    }

    public LogEvent(){
        super.setKey(CommonConstants.EVENT_KEY);
    }

    @Override
    public void setKey(String key) {
        throw new IllegalArgumentException(String.format("Event key name cannot be changed. It will always remain as %s", CommonConstants.EVENT_KEY));

    }
    @Override
    public void setValue (Object value){
        if (!(value instanceof String))
            throw new IllegalArgumentException("value must be a string");
        super.setValue(value);
        this.event = (String) value;

    }

    public static class LogEventBuilder extends LogFieldBuilder{
        LogEventBuilder(){
            super();
        }
    }
}
