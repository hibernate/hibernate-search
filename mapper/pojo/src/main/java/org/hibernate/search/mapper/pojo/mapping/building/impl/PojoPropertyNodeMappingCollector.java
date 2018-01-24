/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

/**
 * @author Yoann Rodiere
 */
public interface PojoPropertyNodeMappingCollector extends PojoNodeMappingCollector {

	void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor fieldModelContributor);

	void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> reference);

	void containedIn();

	void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> pathFilters);

}
