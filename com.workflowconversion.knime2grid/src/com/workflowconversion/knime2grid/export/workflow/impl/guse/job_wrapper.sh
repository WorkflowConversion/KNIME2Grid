#!/usr/bin/env bash
# THIS FILE WAS AUTOMATICALLY GENERATED BY THE KNIME2GUSE KNIME EXTENSION

# contains names of input ports that take filelists
INPUT_PORTS_WITH_FILELIST="@@INPUT_PORTS_WITH_FILELIST@@"
# contains names of output ports that generate filelists
OUTPUT_PORTS_WITH_FILELIST="@@OUTPUT_PORTS_WITH_FILELIST@@"
EXECUTABLE="@@EXECUTABLE@@"

for input_port in ${INPUT_PORTS_WITH_FILELIST}; do
	echo "expanding ${input_port}"
	# we expect the files to be extracted into a folder named after the port
	# e.g.: input_ligands/ligand1.sdf, input_ligands/ligand2.sdf
	echo tar xfz ${input_port}
done

# execute the tool
echo "Executing:" ${EXECUTABLE} $@
${EXECUTABLE} $@

# compress the multi-file outputs
for output_port in ${OUTPUT_PORTS_WITH_FILELIST}; do
	echo "compressing ${output_port}"
	# the whole folder, whose name should match the output port name, will be compressed
	# e.g.: output_score/score1.sdf, output_score/score2.sdf
	echo tar cfz ${output_port}.tmp ${output_port}
	
	# gUSE requires output files to be named as the output port
	rm -Rf ${output_port}
	mv ${output_port}.tmp ${output_port}
done