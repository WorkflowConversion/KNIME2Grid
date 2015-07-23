package com.workflowconversion.knime2guse.exception;

/**
 * Base exception for all exceptions of this plug-in.
 * 
 * @author delagarza
 *
 */
public class KnimeWorkflowConverterException extends RuntimeException {

    private static final long serialVersionUID = 8351762339138478565L;

    public KnimeWorkflowConverterException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public KnimeWorkflowConverterException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    public KnimeWorkflowConverterException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public KnimeWorkflowConverterException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public KnimeWorkflowConverterException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }
    
    

}
