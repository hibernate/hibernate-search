/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HibernateOrmBootstrapIntrospectorAnnotationReadingTest
		extends AbstractHibernateOrmBootstrapIntrospectorPerReflectionStrategyTest {

	@ParameterizedTest(name = "Reflection strategy = {0}")
	@MethodSource("params")
	void singleAnnotation(ValueHandleFactory valueHandleFactory) {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector(
				valueHandleFactory, EntityWithSingleFieldAnnotation.class );
		AnnotationHelper annotationHelper = new AnnotationHelper( introspector.annotationValueHandleFactory() );

		PojoRawTypeModel<EntityWithSingleFieldAnnotation> typeModel =
				introspector.typeModel( EntityWithSingleFieldAnnotation.class );

		PojoPropertyModel<?> propertyModel = typeModel.property( "annotatedProperty" );
		assertThat(
				propertyModel.annotations()
						.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
		)
				.hasSize( 1 )
				.allSatisfy( annotation -> assertThat( annotation )
						.extracting( Annotation::annotationType )
						.isEqualTo( GenericField.class )
				);
	}

	@ParameterizedTest(name = "Reflection strategy = {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3614")
	void repeatedAnnotation(ValueHandleFactory valueHandleFactory) {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector(
				valueHandleFactory, EntityWithRepeatedFieldAnnotation.class );
		AnnotationHelper annotationHelper = new AnnotationHelper( introspector.annotationValueHandleFactory() );

		PojoRawTypeModel<EntityWithRepeatedFieldAnnotation> typeModel =
				introspector.typeModel( EntityWithRepeatedFieldAnnotation.class );

		PojoPropertyModel<?> propertyModel = typeModel.property( "annotatedProperty" );
		assertThat(
				propertyModel.annotations()
						.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
		)
				.hasSize( 2 )
				.allSatisfy( annotation -> assertThat( annotation )
						.extracting( Annotation::annotationType )
						.isEqualTo( GenericField.class )
				);
	}

	@Entity
	private static class EntityWithSingleFieldAnnotation {
		@Id
		private String id;

		@GenericField
		private String annotatedProperty;
	}

	@Entity
	private static class EntityWithRepeatedFieldAnnotation {
		@Id
		private String id;

		@GenericField
		@GenericField(name = "foo")
		private String annotatedProperty;
	}

}
