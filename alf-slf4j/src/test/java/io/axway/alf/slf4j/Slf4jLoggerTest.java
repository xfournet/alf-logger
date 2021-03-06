package io.axway.alf.slf4j;

import java.util.concurrent.atomic.*;
import java.util.function.*;
import javax.annotation.*;
import org.slf4j.event.Level;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import io.axway.alf.Arguments;
import io.axway.alf.exception.FormattedException;
import io.axway.alf.exception.FormattedRuntimeException;
import io.axway.alf.log.Logger;
import io.axway.alf.log.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.event.Level.*;

@SuppressWarnings("Duplicates")
public class Slf4jLoggerTest {

    @DataProvider
    public Object[][] logLevels() {
        return new Object[][]{ //
                {ERROR}, //
                {WARN}, //
                {INFO}, //
                {DEBUG}, //
                {TRACE}, //
        };
    }

    @Test(dataProvider = "logLevels")
    public void simpleLog(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog);
        getMessageMethod(level, logger).accept("Test");

        assertThat(lastLog).hasValue("Test");
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithArguments(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog);
        getMessageAndArgumentMethod(level, logger).accept("Test", a -> a.add("key", "value"));

        assertThat(lastLog).hasValue("Test {\"args\": {\"key\": \"value\"}}");
    }

    @Test(dataProvider = "logLevels")
    public void logWithArgumentsToEscape(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog);
        getMessageAndArgumentMethod(level, logger).accept("Test", a -> a.add("key", "val\"ue"));

        assertThat(lastLog).hasValue("Test {\"args\": {\"key\": \"val\\\"ue\"}}");
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithArgumentsAndThrowable(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();
        AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog, lastThrowable);
        Throwable throwable = new Throwable();
        getMessageArgumentAndExceptionMethod(level, logger).accept("Test", a -> a.add("key", "value"), throwable);

        assertThat(lastLog.get()).startsWith("Test {\"args\": {\"key\": \"value\"}, \"exception\": {\"type\": \"java.lang.Throwable\", \"message\": null}}");
        assertThat(lastThrowable).hasValue(throwable);
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithArgumentsAndExceptionWithArguments(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();
        AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog, lastThrowable);
        Throwable throwable = new FormattedException("Oups", a -> a.add("error", "I did it again"));
        getMessageArgumentAndExceptionMethod(level, logger).accept("Test", a -> a.add("key", "value"), throwable);

        assertThat(lastLog.get()).startsWith(
                "Test {\"args\": {\"key\": \"value\"}, \"exception\": {\"type\": \"io.axway.alf.exception.FormattedException\", \"message\": \"Oups\", \"args\": {\"error\": \"I did it again\"}}}");
        assertThat(lastThrowable).hasValue(throwable);
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithArgumentsAndExceptionWithArgumentsAndCause(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();
        AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog, lastThrowable);
        Throwable throwable = new FormattedException("Oups", a -> a.add("error", "I did it again"), new FormattedRuntimeException("Root cause"));
        getMessageArgumentAndExceptionMethod(level, logger).accept("Test", a -> a.add("key", "value"), throwable);

        assertThat(lastLog.get()).startsWith(
                "Test {\"args\": {\"key\": \"value\"}, \"exception\": {\"type\": \"io.axway.alf.exception.FormattedException\", \"message\": \"Oups\", \"args\": {\"error\": \"I did it again\"}, \"cause\": {\"type\": \"io.axway.alf.exception.FormattedRuntimeException\", \"message\": \"Root cause\"}}}");
        assertThat(lastThrowable).hasValue(throwable);
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithArgumentsAndThrowableWithMessage(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();
        AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog, lastThrowable);
        Throwable throwable = new Throwable("kaboom");
        getMessageArgumentAndExceptionMethod(level, logger).accept("Test", a -> a.add("key", "value"), throwable);

        assertThat(lastLog.get())
                .startsWith("Test {\"args\": {\"key\": \"value\"}, \"exception\": {\"type\": \"java.lang.Throwable\", \"message\": \"kaboom\"}}");
        assertThat(lastThrowable).hasValue(throwable);
    }

    @Test(dataProvider = "logLevels")
    public void simpleLogWithThrowable(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();
        AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog, lastThrowable);
        Throwable throwable = new Throwable();
        getMessageAndThrowableMethod(level, logger).accept("Test", throwable);

        assertThat(lastLog.get()).isEqualTo("Test {\"exception\": {\"type\": \"java.lang.Throwable\", \"message\": null}}");
        assertThat(lastThrowable).hasValue(throwable);
    }

    @Test(dataProvider = "logLevels")
    public void shouldHandleNullValue(Level level) {
        AtomicReference<String> lastLog = new AtomicReference<>();

        Logger logger = createLogger(level, lastLog);

        //noinspection RedundantCast
        getMessageAndArgumentMethod(level, logger).accept("Test", a -> a.add("object", (Object) null).add("string", (String) null));

        assertThat(lastLog).hasValue("Test {\"args\": {\"object\": null, \"string\": null}}");
    }

    @Test
    public void shouldCreateALoggerWithGivenName() {
        Logger logger = LoggerFactory.getLogger("customTest");
        assertThat(logger.getName()).isEqualTo("customTest");
    }

    @Test
    public void shouldCreateALoggerWithGivenClass() {
        Logger logger = LoggerFactory.getLogger(Slf4jLoggerTest.class);
        assertThat(logger.getName()).isEqualTo("io.axway.alf.slf4j.Slf4jLoggerTest");
    }

    private static Logger createLogger(Level level, AtomicReference<String> lastLog) {
        return createLogger(level, lastLog, null);
    }

    private static Logger createLogger(Level level, AtomicReference<String> lastLog, @Nullable AtomicReference<Throwable> lastThrowable) {
        Consumer<Throwable> throwableConsumer = lastThrowable != null ? lastThrowable::set : //
                throwable -> {
                };

        switch (level) {
            case ERROR:
                return new Slf4jLogger(TestLogAdapter.captureError(lastLog::set, throwableConsumer));
            case WARN:
                return new Slf4jLogger(TestLogAdapter.captureWarn(lastLog::set, throwableConsumer));
            case INFO:
                return new Slf4jLogger(TestLogAdapter.captureInfo(lastLog::set, throwableConsumer));
            case DEBUG:
                return new Slf4jLogger(TestLogAdapter.captureDebug(lastLog::set, throwableConsumer));
            case TRACE:
                return new Slf4jLogger(TestLogAdapter.captureTrace(lastLog::set, throwableConsumer));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static Consumer<String> getMessageMethod(Level level, Logger logger) {
        switch (level) {
            case ERROR:
                return logger::error;
            case WARN:
                return logger::warn;
            case INFO:
                return logger::info;
            case DEBUG:
                return logger::debug;
            case TRACE:
                return logger::trace;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static BiConsumer<String, Consumer<Arguments>> getMessageAndArgumentMethod(Level level, Logger logger) {
        switch (level) {
            case ERROR:
                return logger::error;
            case WARN:
                return logger::warn;
            case INFO:
                return logger::info;
            case DEBUG:
                return logger::debug;
            case TRACE:
                return logger::trace;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static BiConsumer<String, Throwable> getMessageAndThrowableMethod(Level level, Logger logger) {
        switch (level) {
            case ERROR:
                return logger::error;
            case WARN:
                return logger::warn;
            case INFO:
                return logger::info;
            case DEBUG:
                return logger::debug;
            case TRACE:
                return logger::trace;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static TriConsumer<String, Consumer<Arguments>, Throwable> getMessageArgumentAndExceptionMethod(Level level, Logger logger) {
        switch (level) {
            case ERROR:
                return logger::error;
            case WARN:
                return logger::warn;
            case INFO:
                return logger::info;
            case DEBUG:
                return logger::debug;
            case TRACE:
                return logger::trace;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}
