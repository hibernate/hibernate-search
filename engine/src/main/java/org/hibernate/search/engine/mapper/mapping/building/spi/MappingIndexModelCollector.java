/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.engine.mapper.processing.spi.ValueProcessor;

/**
 * Interface for mappers to create value processors for bridges.
 * <p>
 * Incidentally, creating the value processors will also generate the index model,
 * due to bridges contributing to the index model as we bind them.
 *
 * @author Yoann Rodiere
 */
public interface MappingIndexModelCollector {

	<T> IdentifierBridge<T> createIdentifierBridge(Class<T> sourceType, BeanReference<? extends IdentifierBridge<?>> reference);

	ValueProcessor addBridge(IndexableModel indexableModel, BridgeDefinition<?> definition);

	ValueProcessor addFunctionBridge(IndexableModel indexableModel, Class<?> sourceType,
			BeanReference<? extends FunctionBridge<?, ?>> bridgeReference,
			String fieldName, FieldModelContributor contributor);

	Optional<MappingIndexModelCollector> addIndexedEmbeddedIfIncluded(IndexedTypeIdentifier parentTypeId,
			String relativePrefix, Integer maxDepth, Set<String> pathFilters);

}
