package de.chojo.sqlutil.wrapper.exception;

import de.chojo.sqlutil.wrapper.QueryBuilder;

import java.sql.SQLException;

/**
 * Exception to wrap {@link Exception} as {@link RuntimeException} thrown during queries executed by {@link QueryBuilder}
 */
public class WrappedQueryExecutionException extends RuntimeException {
    private SQLException cause;

    public WrappedQueryExecutionException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException If the throwable is not a {@link SQLException}
     */
    @Override
    public synchronized Throwable initCause(Throwable cause) {
        this.cause = (SQLException) cause;
        return super.initCause(cause);
    }

    public String getSQLState() {
        return cause.getSQLState();
    }

    public int getErrorCode() {
        return cause.getErrorCode();
    }

    @Override
    public String getMessage() {
        return cause.getMessage();
    }

}
