/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

/**
 * @param <S> The "self" type, i.e. the type to return from methods.
 */
public interface PropertyFieldMappingContext<S extends PropertyFieldMappingContext<?>> extends PropertyMappingContext {

	S projectable(Projectable projectable);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 */
	S valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 */
	S valueBridge(BeanReference<? extends ValueBridge<?, ?>> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 */
	S valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder);

	default S withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( ContainerValueExtractorPath.explicitExtractor( extractorClass ) );
	}

	default S withoutExtractors() {
		return withExtractors( ContainerValueExtractorPath.noExtractors() );
	}

	S withExtractors(ContainerValueExtractorPath extractorPath);

}
