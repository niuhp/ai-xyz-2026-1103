package com.niuhp.aixyz.exception;

/**
 * 仅携带可对外公开错误的业务异常。
 *
 * @author niuhaipeng
 */
public class TicketProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 稳定错误类型。 */
    private final ProcessingError error;

    public TicketProcessingException(ProcessingError error) {
        super(error.getMessage());
        this.error = error;
    }

    public ProcessingError getError() {
        return error;
    }
}
