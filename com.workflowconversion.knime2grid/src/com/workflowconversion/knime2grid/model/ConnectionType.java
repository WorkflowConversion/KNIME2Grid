package com.workflowconversion.knime2grid.model;

/**
 * Each Input/Output has a type that will be determined throughout the conversion process.
 * 
 * @author delagarza
 */
public enum ConnectionType {

	// all connections should start without being assigned
	NotAssigned,
	// connected to an output port
	Channel,
	// data could have been provided and could be modified later on
	UserProvided,
	// collects outputs from a loop
	Collector,
	// generates outputs for a loop
	Generator
}
