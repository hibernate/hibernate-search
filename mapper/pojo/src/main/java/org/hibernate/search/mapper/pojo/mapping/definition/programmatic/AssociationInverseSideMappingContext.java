/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;

/**
 * @author Yoann Rodiere
 */
public interface AssociationInverseSideMappingContext extends PropertyMappingContext {

	/**
	 * @param extractorClass The type of container extractor to use.
	 * @return {@code this}, for method chaining.
	 */
	@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
	default AssociationInverseSideMappingContext withExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorClass ) );
	}

	/**
	 * @param extractorType The type of container extractor to use.
	 * @return {@code this}, for method chaining.
	 */
	default AssociationInverseSideMappingContext withExtractor(BuiltinContainerExtractor extractorType) {
		return withExtractor( extractorType.getType() );
	}

	/**
	 * Indicate that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 */
	default AssociationInverseSideMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 */
	AssociationInverseSideMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
