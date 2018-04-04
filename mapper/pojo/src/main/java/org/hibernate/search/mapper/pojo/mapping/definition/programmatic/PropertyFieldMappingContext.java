/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

/**
 * @author Yoann Rodiere
 */
public interface PropertyFieldMappingContext extends PropertyMappingContext {

	PropertyFieldMappingContext valueBridge(String bridgeName);

	PropertyFieldMappingContext valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass);

	PropertyFieldMappingContext valueBridge(String bridgeName, Class<? extends ValueBridge<?, ?>> bridgeClass);

	PropertyFieldMappingContext valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder);

	PropertyFieldMappingContext analyzer(String analyzerName);

	PropertyFieldMappingContext normalizer(String normalizerName);

	PropertyFieldMappingContext store(Store store);

	PropertyFieldMappingContext sortable(Sortable sortable);

	default PropertyFieldMappingContext withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( ContainerValueExtractorPath.explicitExtractor( extractorClass ) );
	}

	default PropertyFieldMappingContext withoutExtractors() {
		return withExtractors( ContainerValueExtractorPath.noExtractors() );
	}

	PropertyFieldMappingContext withExtractors(ContainerValueExtractorPath extractorPath);

}
