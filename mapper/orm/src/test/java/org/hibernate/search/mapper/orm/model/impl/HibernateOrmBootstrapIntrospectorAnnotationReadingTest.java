/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public class HibernateOrmBootstrapIntrospectorAnnotationReadingTest
		extends AbstractHibernateOrmBootstrapIntrospectorPerReflectionStrategyTest {

	@Test
	public void singleAnnotation() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithSingleFieldAnnotation.class );
		AnnotationHelper annotationHelper = new AnnotationHelper( introspector.getAnnotationValueReadHandleFactory() );

		PojoRawTypeModel<EntityWithSingleFieldAnnotation> typeModel =
				introspector.getTypeModel( EntityWithSingleFieldAnnotation.class );

		PojoPropertyModel<?> propertyModel = typeModel.getProperty( "annotatedProperty" );
		assertThat(
				propertyModel.getAnnotations()
						.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
		)
				.hasSize( 1 )
				.allSatisfy( annotation -> assertThat( annotation )
						.extracting( Annotation::annotationType )
						.isEqualTo( GenericField.class )
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3614")
	public void repeatedAnnotation() {
		HibernateOrmBootstrapIntrospector introspector = createIntrospector( EntityWithRepeatedFieldAnnotation.class );
		AnnotationHelper annotationHelper = new AnnotationHelper( introspector.getAnnotationValueReadHandleFactory() );

		PojoRawTypeModel<EntityWithRepeatedFieldAnnotation> typeModel =
				introspector.getTypeModel( EntityWithRepeatedFieldAnnotation.class );

		PojoPropertyModel<?> propertyModel = typeModel.getProperty( "annotatedProperty" );
		assertThat(
				propertyModel.getAnnotations()
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