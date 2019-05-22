/**
 * Copyright (c) 2013, Luis de la Garza.
 *
 * This file is part of KnimeWorkflowExporter.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.workflowconversion.knime2grid.model;

import org.apache.commons.lang.Validate;
import org.knime.core.node.workflow.NodeID;


/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Input extends Port {

        // the ID of the KNIME node that produced the data that goes into this input
        private NodeID sourceId;
        // the port number of the job that provides data for this input
        private int sourcePortNr;

        /**
         * @param sourceId
         *            the originalSourceId to set
         */
        public void setSourceId(final NodeID sourceId) {
                Validate.notNull(sourceId, "sourceId cannot be null");
                this.sourceId = sourceId;
        }

        /**
         * @return the originalSourceId
         */
        public NodeID getSourceId() {
                return sourceId;
        }

        /**
         * @return the sourcePortNr
         */
        public int getSourcePortNr() {
                return sourcePortNr;
        }

        /**
         * @param sourcePortNr
         *            the sourcePortNr to set
         */
        public void setSourcePortNr(final int sourcePortNr) {
                this.sourcePortNr = sourcePortNr;
        }
}