/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

/**
 * @author Yoann Rodiere
 */
public interface AssociationInverseSideMappingContext extends PropertyMappingContext {

	default AssociationInverseSideMappingContext withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( Collections.singletonList( extractorClass ) );
	}

	AssociationInverseSideMappingContext withExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses);

	AssociationInverseSideMappingContext withoutExtractors();

	AssociationInverseSideMappingContext withoutInverseExtractors();

	default AssociationInverseSideMappingContext withInverseExtractor(
			Class<? extends ContainerValueExtractor> inverseExtractorClass) {
		return withInverseExtractors( Collections.singletonList( inverseExtractorClass ) );
	}

	AssociationInverseSideMappingContext withInverseExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> inverseExtractorClasses);
}
