package com.workflowconversion.knime2grid.model;

/**
 * Defines the basic type of a job.
 */
public enum JobType {
	// collects outputs from a loop
	Collector,
	// generates outputs for a loop
	Generator,
	// represents a KNIME node
	KnimeInternal,
	// job represents a command-line tool
	CommandLine
}
