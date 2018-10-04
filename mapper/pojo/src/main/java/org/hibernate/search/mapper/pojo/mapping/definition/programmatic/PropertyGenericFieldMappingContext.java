/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

/**
 * @author Yoann Rodiere
 */
public interface PropertyGenericFieldMappingContext extends PropertyMappingContext {

	PropertyGenericFieldMappingContext valueBridge(String bridgeName);

	PropertyGenericFieldMappingContext valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass);

	PropertyGenericFieldMappingContext valueBridge(String bridgeName, Class<? extends ValueBridge<?, ?>> bridgeClass);

	PropertyGenericFieldMappingContext valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder);

	PropertyGenericFieldMappingContext analyzer(String analyzerName);

	PropertyGenericFieldMappingContext normalizer(String normalizerName);

	PropertyGenericFieldMappingContext store(Store store);

	PropertyGenericFieldMappingContext sortable(Sortable sortable);

	default PropertyGenericFieldMappingContext withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( ContainerValueExtractorPath.explicitExtractor( extractorClass ) );
	}

	default PropertyGenericFieldMappingContext withoutExtractors() {
		return withExtractors( ContainerValueExtractorPath.noExtractors() );
	}

	PropertyGenericFieldMappingContext withExtractors(ContainerValueExtractorPath extractorPath);

}
