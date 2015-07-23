package com.workflowconversion.knime2guse.exception;

import org.knime.core.node.NodeFactory;

/**
 * Thrown when the class of a specific {@link NodeFactory} cannot be found.
 * @author delagarza
 *
 */
public class NodeFactoryClassNotFoundException extends
        KnimeWorkflowConverterException {

    private static final long serialVersionUID = 8238909906803180064L;

    public NodeFactoryClassNotFoundException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public NodeFactoryClassNotFoundException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    public NodeFactoryClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public NodeFactoryClassNotFoundException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public NodeFactoryClassNotFoundException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
