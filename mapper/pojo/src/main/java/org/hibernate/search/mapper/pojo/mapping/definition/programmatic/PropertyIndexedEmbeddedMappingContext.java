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
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

/**
 * A context to define how a property is indexed-embedded.
 */
public interface PropertyIndexedEmbeddedMappingContext extends PropertyMappingContext {

	/**
	 * @param prefix The prefix used when embedding.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#prefix()
	 */
	PropertyIndexedEmbeddedMappingContext prefix(String prefix);

	/**
	 * @param depth The max recursion depth for indexed-embedded processing.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#maxDepth()
	 */
	PropertyIndexedEmbeddedMappingContext maxDepth(Integer depth);

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	default PropertyIndexedEmbeddedMappingContext includePaths(String ... paths) {
		return includePaths( Arrays.asList( paths ) );
	}

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	PropertyIndexedEmbeddedMappingContext includePaths(Collection<String> paths);

	/**
	 * @param storage The storage strategy of the object field created for this indexed-embedded.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#storage()
	 * @see ObjectFieldStorage
	 */
	PropertyIndexedEmbeddedMappingContext storage(ObjectFieldStorage storage);

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default PropertyIndexedEmbeddedMappingContext withExtractor(String extractorName) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 */
	default PropertyIndexedEmbeddedMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 * @see ContainerExtractorPath
	 */
	PropertyIndexedEmbeddedMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
