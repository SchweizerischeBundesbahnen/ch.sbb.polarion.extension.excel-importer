package ch.sbb.polarion.extension.excel_importer.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExceptionUtilsTest {

    @Test
    void shouldReturnMessageFromSimpleException() {
        Exception e = new IllegalArgumentException("simple error");
        assertEquals("simple error", ExceptionUtils.getRootCauseMessage(e));
    }

    @Test
    void shouldReturnRootCauseMessageFromNestedExceptions() {
        Exception root = new IllegalArgumentException("File doesn't contain sheet 'Tabelle1'");
        Exception mid = new java.lang.reflect.InvocationTargetException(root);
        Exception top = new java.util.concurrent.CompletionException(mid);
        assertEquals("File doesn't contain sheet 'Tabelle1'", ExceptionUtils.getRootCauseMessage(top));
    }

    @Test
    void shouldReturnNullMessageWhenRootHasNoMessage() {
        Exception root = new RuntimeException((String) null);
        Exception wrapper = new Exception(root);
        assertNull(ExceptionUtils.getRootCauseMessage(wrapper));
    }

    @Test
    void shouldHandleSelfReferencingCause() {
        Exception e = new SelfCausedException("self-referencing");
        assertEquals("self-referencing", ExceptionUtils.getRootCauseMessage(e));
    }

    private static class SelfCausedException extends Exception {
        SelfCausedException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }
}
