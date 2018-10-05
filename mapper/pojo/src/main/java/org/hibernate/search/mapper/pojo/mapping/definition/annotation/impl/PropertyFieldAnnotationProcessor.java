/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBuilderBeanReference;
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
				getValueBridgeBuilder( annotation ),
				propertyModel
		);
		fieldContext.valueBridge( builder );

		ContainerValueExtractorPath extractorPath =
				helper.getExtractorPath( getExtractors( annotation ) );
		fieldContext.withExtractors( extractorPath );

		Store store = getStore( annotation );
		if ( !Store.DEFAULT.equals( store ) ) {
			fieldContext.store( store );
		}
	}

	abstract PropertyFieldMappingContext<?> initFieldMappingContext(PropertyMappingContext mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract String getName(A annotation);

	abstract Store getStore(A annotation);

	abstract ValueBridgeBeanReference getValueBridge(A annotation);

	abstract ValueBridgeBuilderBeanReference getValueBridgeBuilder(A annotation);

	abstract ContainerValueExtractorBeanReference[] getExtractors(A annotation);
}
