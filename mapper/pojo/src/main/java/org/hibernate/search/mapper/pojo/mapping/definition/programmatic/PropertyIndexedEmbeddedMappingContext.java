/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;

public interface PropertyIndexedEmbeddedMappingContext extends PropertyMappingContext {

	PropertyIndexedEmbeddedMappingContext prefix(String prefix);

	PropertyIndexedEmbeddedMappingContext storage(ObjectFieldStorage storage);

	PropertyIndexedEmbeddedMappingContext maxDepth(Integer depth);

	default PropertyIndexedEmbeddedMappingContext includePaths(String ... paths) {
		return includePaths( Arrays.asList( paths ) );
	}

	PropertyIndexedEmbeddedMappingContext includePaths(Collection<String> paths);

	/**
	 * @param extractorClass The type of container extractor to use.
	 * @return {@code this}, for method chaining.
	 */
	@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
	default PropertyIndexedEmbeddedMappingContext withExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorClass ) );
	}

	/**
	 * @param extractorType The type of container extractor to use.
	 * @return {@code this}, for method chaining.
	 */
	default PropertyIndexedEmbeddedMappingContext withExtractor(BuiltinContainerExtractor extractorType) {
		return withExtractor( extractorType.getType() );
	}

	/**
	 * Indicate that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 */
	default PropertyIndexedEmbeddedMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 */
	PropertyIndexedEmbeddedMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
