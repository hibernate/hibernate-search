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
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

/**
 * The step in a property-to-indexed-embedded mapping where optional parameters can be set.
 */
public interface PropertyMappingIndexedEmbeddedStep extends PropertyMappingStep {

	/**
	 * @param prefix The prefix used when embedding.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#prefix()
	 */
	PropertyMappingIndexedEmbeddedStep prefix(String prefix);

	/**
	 * @param depth The max recursion depth for indexed-embedded processing.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#maxDepth()
	 */
	PropertyMappingIndexedEmbeddedStep maxDepth(Integer depth);

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	default PropertyMappingIndexedEmbeddedStep includePaths(String ... paths) {
		return includePaths( Arrays.asList( paths ) );
	}

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	PropertyMappingIndexedEmbeddedStep includePaths(Collection<String> paths);

	/**
	 * @param storage The storage strategy of the object field created for this indexed-embedded.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#storage()
	 * @see ObjectFieldStorage
	 */
	PropertyMappingIndexedEmbeddedStep storage(ObjectFieldStorage storage);

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default PropertyMappingIndexedEmbeddedStep extractor(String extractorName) {
		return extractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 */
	default PropertyMappingIndexedEmbeddedStep extractors() {
		return extractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 * @see ContainerExtractorPath
	 */
	PropertyMappingIndexedEmbeddedStep extractors(ContainerExtractorPath extractorPath);

}
