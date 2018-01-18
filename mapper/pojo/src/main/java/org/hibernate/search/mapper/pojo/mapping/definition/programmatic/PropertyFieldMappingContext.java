/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;

/**
 * @author Yoann Rodiere
 */
public interface PropertyFieldMappingContext extends PropertyMappingContext {

	PropertyFieldMappingContext name(String name);

	PropertyFieldMappingContext functionBridge(String bridgeName);

	PropertyFieldMappingContext functionBridge(Class<? extends FunctionBridge<?, ?>> bridgeClass);

	PropertyFieldMappingContext functionBridge(String bridgeName, Class<? extends FunctionBridge<?, ?>> bridgeClass);

	PropertyFieldMappingContext functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder);

	PropertyFieldMappingContext store(Store store);

}
