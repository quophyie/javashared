package com.quantal.javashared.logger;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggingConfigs;
import com.godaddy.logging.logger.AnnotatingLogger;
import com.godaddy.logging.logger.LoggerImpl;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LogEvent;
import com.quantal.javashared.dto.LogzioConfig;
import com.quantal.javashared.exceptions.EventNotSuppliedException;
import io.logz.sender.LogzioSender;
import io.logz.sender.com.google.gson.Gson;
import io.logz.sender.com.google.gson.JsonObject;
import io.logz.sender.exceptions.LogzioParameterErrorException;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by dman on 19/07/2017.
 */
public class QuantalGoDaddyLoggerImpl extends LoggerImpl implements QuantalGoDaddyLogger {

    protected boolean hasEvent;
    protected LogzioSender sender;
    protected LogzioConfig logzioConfig;

    private final String EVENT_MSG="Event not supplied. Please supply an event via the %s method or with an 'event' key in the 'with' method";
    private final String EVENT_KEY = "event";
    private boolean bSendToLogzio = false;
    private Map<String, Object> logzioJsonDataMap = new HashMap<>();
    private JsonObject jsonMessage;
    private final Map<String, Object> commonFieldsMap;
    private CommonLogFields commonLogFields;


    public QuantalGoDaddyLoggerImpl(Logger root, LoggingConfigs configs, LogzioConfig logzioConfig) {
        super(root, configs);
        this.logzioConfig = logzioConfig;
        commonFieldsMap = new HashMap<>();

        if (logzioConfig != null)
            bSendToLogzio = true;

        if(bSendToLogzio) {
            try {
                sender = LogzioSender.getOrCreateSenderByType(logzioConfig.getLogzioToken(),
                        logzioConfig.getLogzioType(),
                        logzioConfig.getDrainTimeout(),
                        logzioConfig.getFsPercentThreshold(),
                        logzioConfig.getBufferDir(),
                        logzioConfig.getLogzioUrl(),
                        logzioConfig.getSocketTimeout(),
                        logzioConfig.getConnectTimeout(),
                        logzioConfig.isDebug(),
                        logzioConfig.getReporter(), logzioConfig.getTasksExecutor(),
                        logzioConfig.getGcPersistedQueueFilesIntervalSeconds());
                sender.start();
            } catch (LogzioParameterErrorException e) {
                root.error(e.getMessage(), e);
            }

        }


    }

    public QuantalGoDaddyLoggerImpl(Logger root, LoggingConfigs configs) {
        this(root, configs, QuantalGoDaddyLoggerFactory.createDefaultLogzioConfig("LOGIO_TOKEN"));

    }

    @Override
    public Logger with(Object obj) {
        if(obj instanceof LogEvent && !StringUtils.isEmpty(((LogEvent) obj).getEvent())){

            this.hasEvent = true;
        }

        if (obj instanceof Map){
            addToLogzioJsonMessage(Arrays.asList(obj));
        }
        else {
            addToLogzioDataMap(obj);
        }
        return new AnnotatingLogger(root, this, obj, configs);
    }

    @Override
    public Logger with(final String key, final Object value) {
        if (!StringUtils.isEmpty(key)) {

            if (!"event".equals(key.toLowerCase().trim())) {
                hasEvent = false;
            } else {
                hasEvent = true;

            }
        }

        if (jsonMessage == null)
            createLogMessage();
        jsonMessage.addProperty(key, value.toString());
        return super.with(key, value);
    }

    @Override
    public void trace(String msg) {
        checkAndMaybeSendToELK(msg,"trace", null);
        super.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(arg));
        super.trace(format, arg);
    }


    @Override
    public void trace(String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(arg1, arg2));
        super.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(arguments));
        super.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
        .trace(msg, t);
    }


    @Override
    public void trace(Marker marker, String msg) {
        checkAndMaybeSendToELK(msg,"trace",null);;
        super.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(arg));
        super.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(arg1, arg2));
        super.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        checkAndMaybeSendToELK(format,"trace", Arrays.asList(argArray));
        super.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .trace(marker,msg, t);
    }


    @Override
    public void debug(String msg) {
        checkAndMaybeSendToELK(msg,"debug", null);
        super.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(arg));
        super.debug(format, arg);
    }


    @Override
    public void debug(String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(arg1, arg2));
        super.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(arguments));
        super.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .debug(msg, t);
    }


    @Override
    public void debug(Marker marker, String msg) {
        checkAndMaybeSendToELK(msg,"debug",null);;
        super.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(arg));
        super.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(arg1, arg2));
        super.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        checkAndMaybeSendToELK(format,"debug", Arrays.asList(argArray));
        super.debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .debug(marker,msg, t);
    }


/*******/


@Override
public void info(String msg) {
    checkAndMaybeSendToELK(msg,"info", null);
    super.info(msg);
}

    @Override
    public void info(String format, Object arg) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(arg));
        super.info(format, arg);
    }


    @Override
    public void info(String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(arg1, arg2));
        super.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(arguments));
        super.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .info(msg, t);
    }


    @Override
    public void info(Marker marker, String msg) {
        checkAndMaybeSendToELK(msg,"info",null);;
        super.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(arg));
        super.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(arg1, arg2));
        super.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        checkAndMaybeSendToELK(format,"info", Arrays.asList(argArray));
        super.info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .info(marker,msg, t);
    }


    /**** WARN **/
    @Override
    public void warn(String msg) {
        checkAndMaybeSendToELK(msg,"warn", null);
        super.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(arg));
        super.warn(format, arg);
    }


    @Override
    public void warn(String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(arg1, arg2));
        super.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(arguments));
        super.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .warn(msg, t);
    }


    @Override
    public void warn(Marker marker, String msg) {
        checkAndMaybeSendToELK(msg,"warn",null);;
        super.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(arg));
        super.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(arg1, arg2));
        super.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(argArray));
        super.warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .warn(marker,msg, t);
    }

    /*** END OF WARN ***/

    /**
     *
     * ERROR
     */
    @Override
    public void error(String msg) {
        checkAndMaybeSendToELK(msg,"error", null);
        super.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(arg));
        super.error(format, arg);
    }


    @Override
    public void error(String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(arg1, arg2));
        super.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(arguments));
        super.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .error(msg, t);
    }


    @Override
    public void error(Marker marker, String msg) {
        checkAndMaybeSendToELK(msg,"error",null);;
        super.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(arg));
        super.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(arg1, arg2));
        super.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(argArray));
        super.error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        this.with(EVENT_KEY, t.getClass().getName())
                .error(marker,msg, t);
    }

    @Override
    public void error(Throwable t, String format, Object... args) {
        checkAndMaybeSendToELK(format,"error", Arrays.asList(args));
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        root.error(ft.getMessage(), t);
    }

    @Override
    public void warn(final Throwable t, final String format, final Object... args) {
        checkAndMaybeSendToELK(format,"warn", Arrays.asList(args));
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        root.warn(ft.getMessage(), t);
    }

    @Override public void success(final String format, final Object... args) {
        info(successMarker, format, args);
    }

    @Override public void dashboard(final String format, final Object... args) {
        info(dashboardMarker, format, args);
    }

    private void checkAndMaybeSendToELK(String msg, String methodName, List<Object> arguments){
        checkAndMaybeThrowEventNotSuppliedException(methodName, arguments);
        checkAndSendToLogzio(msg, arguments);
    }

    private void checkAndMaybeThrowEventNotSuppliedException(String methodName, List<Object> arguments){
        if (arguments !=null) {
            Object event = arguments.stream().filter(arg -> arg instanceof LogEvent).findAny().orElse(null);


            if (!this.hasEvent && event == null){
                throw new EventNotSuppliedException(String.format(EVENT_MSG, methodName));
            }
            if(event!=null) {
                this.with(EVENT_KEY, ((LogEvent) event).getEvent());
            }
        }

        else if(!this.hasEvent) {
            throw new EventNotSuppliedException(String.format(EVENT_MSG, methodName));
        }
    }
    private void checkAndSendToLogzio(String msg, List<Object> args){
        if (!bSendToLogzio)
            return;
        String formattedMsg = getFormattedMessage(msg, args.toArray());
        jsonMessage = addToLogzioJsonMessage(args);
        logzioJsonDataMap.putAll(commonFieldsMap);
        logzioJsonDataMap.putIfAbsent("msg", formattedMsg);

        if(args!=null) {
            args.forEach(this::addToLogzioDataMap);
        }

        sender.send(jsonMessage);

        logzioJsonDataMap = new HashMap<>();
        createLogMessage();
    }

    private void addToLogzioDataMap(Object obj){
        String key = obj.getClass().getSimpleName();

        if(!logzioJsonDataMap.containsKey(key)){
            Matcher matcher =  Pattern.compile("\\d*$").matcher(key);
            if (matcher.find()){
                String group = matcher.group();
                long objTypeCnt = StringUtils.isEmpty(group) ? 0L : Long.valueOf(group);
                String newKey = key.substring(0, group.indexOf(group)).concat(String.valueOf(++objTypeCnt));
                if(obj instanceof LogEvent) {
                    logzioJsonDataMap.putIfAbsent(EVENT_KEY, ((LogEvent)obj).getEvent());
                } else {
                    logzioJsonDataMap.put(key.concat(newKey), obj);
                }
                addToLogzioJsonMessage(Arrays.asList(logzioJsonDataMap));
            }
        }
    }

    private JsonObject addToLogzioJsonMessage(List<Object> args){

        if (args != null ) {
            args.stream().forEach(arg -> {
                if (arg instanceof Map) {

                    ((Map) arg).entrySet().forEach((entry) -> {
                        String json = new Gson().toJson(((Map.Entry<String, Object>) entry).getValue());
                        if (jsonMessage == null)
                            createLogMessage();
                        jsonMessage.addProperty(((Map.Entry<String, Object>) entry).getKey(), json);
                    });
                }
            });
        }
        return jsonMessage;
    }

    private JsonObject createLogMessage(){
        jsonMessage =  new JsonObject();
        return jsonMessage;

    }

    private String getFormattedMessage(String msg, Object... args){
        return MessageFormatter.arrayFormat(msg, args).getMessage();
    }

    @Override
    public CommonLogFields getCommoFields() {
        return this.commonLogFields;
    }

    @Override
    public void setCommoFields(CommonLogFields commonLogFields) {
        this.commonLogFields = commonLogFields;
        Arrays.asList(ReflectionUtils.getAllDeclaredMethods(commonLogFields.getClass()))
                .stream()
                .filter(method -> method.getName().startsWith("get") &&  !method.getName().equalsIgnoreCase("getClass"))
                .forEach(method -> {
                    try {
                        String key = method.getName().substring(3).toLowerCase();
                        Object value = method.invoke(commonLogFields);
                        commonFieldsMap.put(key, value);
                    } catch (IllegalAccessException | InvocationTargetException  e) {
                       root.error(e.getMessage(), e );
                    }
                });
    }
}