/*
 * Copyright (c) 2012 ISPRAS
 * 
 * Institute for System Programming of Russian Academy of Sciences
 * 
 * 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
 * 
 * All rights reserved.
 * 
 * IOperation.java, Nov 9, 2012 8:01:52 PM Andrei Tatarnikov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.model.api.instruction;

import java.util.Collection;
import java.util.Map;

import ru.ispras.microtesk.model.api.metadata.MetaOperation;

/**
 * The IOperation interface is the base interfaces for operations
 * described by Op statements in the Sim-nML language. 
 * 
 * @author Andrei Tatarnikov
 */

public interface IOperation extends IPrimitive
{
    public interface IFactory
    {
        public IOperation create(Map<String, Object> args);
    }
    
    /**
     * The IInfo interface provides information on an operation object or
     * a group of operation object united by an OR rule. This information
     * is needed for runtime checks to make sure that instructions are
     * configured with proper operation objects.
     * 
     * @author Andrei Tatarnikov
     */

    public interface IInfo
    {
        /**
         * Returns the name of the operation or the name of the OR rule used
         * for grouping operations.
         * 
         * @return The mode name.
         */

        public String getName();

        public Map<String, IOperationBuilder> createBuilders();
        public Map<String, IOperationBuilder> createBuildersForShortcut(String contextName);

        /**
         * Checks if the current operation (or group of operations) implements
         * (or contains) the specified operation. This method is used in runtime
         * checks to make sure that the object composition in the model is valid.   
         * 
         * @param op An operation object.
         * @return true if the operation is supported or false otherwise. 
         */

        public boolean isSupported(IOperation op);

        /**
         * Returns a collection of meta data objects describing the operation
         * (or the group of operations) the info object refers to.
         * In the case, when there is a single operation, the collection
         * contains only one item.   
         * 
         * @return A collection of meta data objects for an operation or a group of 
         * operations.  
         */

        public Collection<MetaOperation> getMetaData();
    }
}
