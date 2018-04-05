/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;

/**
 * @author Yoann Rodiere
 */
public interface AssociationInverseSideMappingContext extends PropertyMappingContext {

	default AssociationInverseSideMappingContext withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( ContainerValueExtractorPath.explicitExtractor( extractorClass ) );
	}

	default AssociationInverseSideMappingContext withoutExtractors() {
		return withExtractors( ContainerValueExtractorPath.noExtractors() );
	}

	AssociationInverseSideMappingContext withExtractors(ContainerValueExtractorPath extractorPath);

}
