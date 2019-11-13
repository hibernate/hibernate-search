/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class AssociationInverseSideProcessor extends PropertyAnnotationProcessor<AssociationInverseSide> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	AssociationInverseSideProcessor(AnnotationProcessorHelper helper) {
		super( helper );
	}

	@Override
	public Stream<? extends AssociationInverseSide> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( AssociationInverseSide.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext,
			AssociationInverseSide annotation) {
		ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extraction() );

		Optional<PojoModelPathValueNode> inversePathOptional =
				helper.getPojoModelPathValueNode( annotation.inversePath() );
		if ( !inversePathOptional.isPresent() ) {
			throw log.missingInversePathInAssociationInverseSideMapping();
		}

		mappingContext.associationInverseSide( inversePathOptional.get() )
				.extractors( extractorPath );
	}
}
