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
package com.workflowconversion.knime2guse.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.Validate;


/**
 * 
 * 
 * @author Luis de la Garza
 */
public class Output extends Port {

        private final List<Destination> destinations = new LinkedList<Destination>();

        /**
         * 
         * @param destination
         *            destination.
         */
        public void addDestination(final Destination destination) {
                destinations.add(destination);
        }

        /**
         * @return the destinations
         */
        public Collection<Destination> getDestinations() {
                return destinations;
        }

        public void clearDestinations() {
                destinations.clear();
        }

        public static class Destination {
                private Job target;
                private int targetPortNr;

                public Destination(final Job target, final int targetPortNr) {
                        Validate.notNull(target, "target cannot be null");
                        Validate.isTrue(targetPortNr > -1, "targetPortNr cannot be negative", targetPortNr);
                        this.target = target;
                        this.targetPortNr = targetPortNr;
                }

                /**
                 * @return the target
                 */
                public Job getTarget() {
                        return target;
                }

                /**
                 * @return the targetPortNr
                 */
                public int getTargetPortNr() {
                        return targetPortNr;
                }

                public void setTarget(final Job target) {
                        this.target = target;
                }

                public void setTargetPortNr(final int targetPortNr) {
                        this.targetPortNr = targetPortNr;
                }

        }

}