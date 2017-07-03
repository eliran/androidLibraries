package com.threeplay.core;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Created by eliranbe on 6/10/16.
 */
public class LoggerTest {
    private String testMessage = "test message";
    private Logger logger;
    private Logger.Reporter mockReporter;
    private Logger mockDefaultLogger;

    @Before
    public void setup(){
        logger = new Logger();
        mockDefaultLogger = Mockito.mock(Logger.class);
        mockReporter = Mockito.mock(Logger.Reporter.class);
        logger.attachReporter(mockReporter);
        Logger.setDefaultLogger(mockDefaultLogger);
    }

    @Test
    public void logger_can_write_a_message(){
        logger.log(Logger.DEBUG, testMessage);
        verify(mockReporter).log(Logger.DEBUG, testMessage);
    }

    @Test
    public void logger_pass_message_to_all_reporters(){
        Logger.Reporter anotherReporter = Mockito.mock(Logger.Reporter.class);
        logger.attachReporter(anotherReporter);
        logger.log(Logger.ALL, testMessage);
        verify(mockReporter).log(Logger.ALL, testMessage);
        verify(anotherReporter).log(Logger.ALL, testMessage);
    }

    @Test
    public void logger_with_debug_level(){
        logger.debug(testMessage);
        verify(mockReporter).log(Logger.DEBUG, testMessage);
    }

    @Test
    public void logger_with_error_level(){
        logger.error(testMessage);
        verify(mockReporter).log(Logger.ERROR, testMessage);
    }

    @Test
    public void logger_with_warn_level(){
        logger.warning(testMessage);
        verify(mockReporter).log(Logger.WARN, testMessage);
    }

    @Test
    public void logger_with_info_level(){
        logger.info(testMessage);
        verify(mockReporter).log(Logger.INFO, testMessage);
    }

    @Test
    public void logger_with_arguments(){
        logger.info("test %d", 1);
        verify(mockReporter).log(anyInt(), eq("test 1"));
    }

    @Test
    public void static_log_info_passes_to_default_logger(){
        Logger.i(testMessage);
        verify(mockDefaultLogger).info(testMessage);
    }

    @Test
    public void static_log_warn_passes_to_default_logger(){
        Logger.w(testMessage);
        verify(mockDefaultLogger).warning(testMessage);
    }

    @Test
    public void static_log_debug_passes_to_default_logger(){
        Logger.d(testMessage);
        verify(mockDefaultLogger).debug(testMessage);
    }

    @Test
    public void static_log_error_passes_to_default_logger(){
        Logger.e(testMessage);
        verify(mockDefaultLogger).error(testMessage);
    }

    @Test
    public void static_log_passes_to_default_logger(){
        Logger.l(Logger.ALL, testMessage);
        verify(mockDefaultLogger).log(Logger.ALL, testMessage);
    }

    @Test
    public void defaultLogger_is_available_for_interaction(){
        assertThat(Logger.defaultLogger(), is(mockDefaultLogger));
    }

}
