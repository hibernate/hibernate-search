/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.annotation.Search5DeprecatedAPI;

/**
 * The step in a property-to-indexed-embedded mapping where optional parameters can be set.
 */
public interface PropertyMappingIndexedEmbeddedStep extends PropertyMappingStep {

	/**
	 * @param prefix The prefix used when embedding.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#prefix()
	 * @deprecated Pass a field name to {@link PropertyMappingStep#indexedEmbedded(String)} instead.
	 * Note that the name passed to that method does not allow dots.
	 */
	@Deprecated
	@Search5DeprecatedAPI
	PropertyMappingIndexedEmbeddedStep prefix(String prefix);

	/**
	 * @param depth The number of levels of indexed-embedded that will have all their fields included by default.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includeDepth()
	 */
	PropertyMappingIndexedEmbeddedStep includeDepth(Integer depth);

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	default PropertyMappingIndexedEmbeddedStep includePaths(String... paths) {
		return includePaths( Arrays.asList( paths ) );
	}

	/**
	 * @param paths The paths of index fields to include explicitly.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includePaths()
	 */
	PropertyMappingIndexedEmbeddedStep includePaths(Collection<String> paths);

	/**
	 * @param paths The paths of index fields to exclude.
	 * @return {@code this}, for method chaining.
	 *
	 * @see IndexedEmbedded#excludePaths()
	 */
	@Incubating
	default PropertyMappingIndexedEmbeddedStep excludePaths(String... paths) {
		return excludePaths( Arrays.asList( paths ) );
	}

	/**
	 * @param paths The paths of index fields to exclude.
	 * @return {@code this}, for method chaining.
	 *
	 * @see IndexedEmbedded#excludePaths()
	 */
	@Incubating
	PropertyMappingIndexedEmbeddedStep excludePaths(Collection<String> paths);

	/**
	 * @param include Whether the identifier of embedded objects should be included as an index field.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#includeEmbeddedObjectId()
	 */
	PropertyMappingIndexedEmbeddedStep includeEmbeddedObjectId(boolean include);

	/**
	 * @param structure How the structure of the object field created for this indexed-embedded
	 * is preserved upon indexing.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#structure()
	 * @see ObjectStructure
	 */
	PropertyMappingIndexedEmbeddedStep structure(ObjectStructure structure);

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
	default PropertyMappingIndexedEmbeddedStep noExtractors() {
		return extractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#extraction()
	 * @see ContainerExtractorPath
	 */
	PropertyMappingIndexedEmbeddedStep extractors(ContainerExtractorPath extractorPath);

	/**
	 * @param targetType A type indexed-embedded elements should be cast to.
	 * When relying on {@link #extractors(ContainerExtractorPath) container extraction},
	 * the extracted values are cast, not the container.
	 * By default, no casting occurs.
	 * @return {@code this}, for method chaining.
	 * @see IndexedEmbedded#targetType()
	 */
	PropertyMappingIndexedEmbeddedStep targetType(Class<?> targetType);

}
