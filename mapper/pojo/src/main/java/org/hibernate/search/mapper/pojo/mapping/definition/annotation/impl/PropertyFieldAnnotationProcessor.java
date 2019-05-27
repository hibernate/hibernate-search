/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

abstract class PropertyFieldAnnotationProcessor<A extends Annotation> extends PropertyAnnotationProcessor<A> {
	private final Class<A> annotationType;

	PropertyFieldAnnotationProcessor(AnnotationProcessorHelper helper, Class<A> annotationType) {
		super( helper );
		this.annotationType = annotationType;
	}

	@Override
	final Stream<? extends A> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( annotationType );
	}

	@Override
	final void doProcess(PropertyMappingContext mappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			A annotation) {
		String cleanedUpRelativeFieldName = getName( annotation );
		if ( cleanedUpRelativeFieldName.isEmpty() ) {
			cleanedUpRelativeFieldName = null;
		}

		PropertyFieldMappingContext<?> fieldContext =
				initFieldMappingContext( mappingContext, propertyModel, annotation, cleanedUpRelativeFieldName );

		BridgeBuilder<? extends ValueBridge<?, ?>> builder = helper.createValueBridgeBuilder(
				getValueBridge( annotation ),
				propertyModel
		);
		fieldContext.valueBridge( builder );

		ContainerExtractorPath extractorPath =
				helper.getExtractorPath( getExtraction( annotation ) );
		fieldContext.withExtractors( extractorPath );

		Projectable projectable = getProjectable( annotation );
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			fieldContext.projectable( projectable );
		}

		Searchable searchable = getSearchable( annotation );
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			fieldContext.searchable( searchable );
		}
	}

	abstract PropertyFieldMappingContext<?> initFieldMappingContext(PropertyMappingContext mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract String getName(A annotation);

	abstract Projectable getProjectable(A annotation);

	abstract Searchable getSearchable(A annotation);

	abstract ValueBridgeRef getValueBridge(A annotation);

	abstract ContainerExtraction getExtraction(A annotation);
}
