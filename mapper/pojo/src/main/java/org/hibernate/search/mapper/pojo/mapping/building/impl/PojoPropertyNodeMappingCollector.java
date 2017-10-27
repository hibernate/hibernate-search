/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Set;

import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;

/**
 * @author Yoann Rodiere
 */
public interface PojoPropertyNodeMappingCollector extends PojoNodeMappingCollector {

	void functionBridge(BeanReference<? extends FunctionBridge<?, ?>> reference,
			String fieldName, FieldModelContributor fieldModelContributor);

	void identifierBridge(BeanReference<IdentifierBridge<?>> bridgeReference);

	void containedIn();

	void indexedEmbedded(String relativePrefix, Integer maxDepth, Set<String> pathFilters);

}
