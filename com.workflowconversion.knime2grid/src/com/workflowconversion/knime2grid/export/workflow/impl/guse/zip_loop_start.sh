#!/usr/bin/env bash
# THIS FILE WAS AUTOMATICALLY GENERATED BY THE KNIME2GUSE KNIME EXTENSION

# we know that the port name refers to an archive (e.g., foo.tar.gz)
INPUT_PORT_NAME="@@INPUT_PORT_NAME@@"
OUTPUT_BASE_NAME="@@OUTPUT_BASE_NAME@@" 

# gUSE expects files from a generator to be named, e.g., bar_0, bar_1, ...
# "specialize in distributed systems", they said; "it will be fun", they said
FILENAME_INDEX=0
for input_file in `tar tfz ${INPUT_PORT_NAME}`; do
	tar xvfOz ${INPUT_PORT_NAME} ${input_file} > ${OUTPUT_BASE_NAME}_${FILENAME_INDEX}
	FILENAME_INDEX=$(expr ${FILENAME_INDEX} + 1)
done