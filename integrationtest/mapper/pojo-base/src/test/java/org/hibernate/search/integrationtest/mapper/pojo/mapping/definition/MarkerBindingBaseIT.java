/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.binding.BindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.MarkerBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Rule;
import org.junit.Test;

public class MarkerBindingBaseIT {

	private static final String INDEX_NAME = "IndexName";

	private static PojoModelType extractedModelType;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	@Test
	public void withParams_annotationMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
		} );
		setupHelper.start().expectCustomBeans().setup( AnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		verifyExtractedModelType();
	}

	@Test
	public void withParams_programmaticMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
		} );
		setupHelper.start()
				.withConfiguration( builder -> {
					builder.addEntityType( NonAnnotatedEntity.class );

					TypeMappingStep indexedEntity = builder.programmaticMapping().type( NonAnnotatedEntity.class )
							.binder( new ExtractTypeModelBinder() );
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "scale3Property" ).marker( new ParametricBinder(),
							Collections.singletonMap( "scale", 3 ) );
					indexedEntity.property( "scale2Property" ).marker( new ParametricBinder(),
							Collections.singletonMap( "scale", 2 ) );
				} )
				.expectCustomBeans().setup( NonAnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		verifyExtractedModelType();
	}

	public static class ParametricBinder implements MarkerBinder {

		@Override
		public void bind(MarkerBindingContext context) {
			context.marker( new ScaleMarker( extractScale( context ) ) );
		}

		@SuppressWarnings("uncheked")
		private static int extractScale(BindingContext context) {
			Optional<Object> optionalScale = context.paramOptional( "scale" );
			if ( optionalScale.isPresent() ) {
				return (Integer) optionalScale.get();
			}

			String stringScale = (String) context.param( "stringScale" );
			return Integer.parseInt( stringScale );
		}
	}

	public static class ScaleMarker {
		final int scale;

		ScaleMarker(int scale) {
			this.scale = scale;
		}
	}

	public static class ExtractTypeModelBinder implements TypeBinder {

		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().useRootOnly();
			extractedModelType = context.bridgedElement();
			context.bridge( (target, bridgedElement, context1) -> {
				// nothing to do
			} );
		}
	}

	@Indexed(index = INDEX_NAME)
	@TypeBinding(binder = @TypeBinderRef(type = ExtractTypeModelBinder.class))
	public static class AnnotatedEntity {
		@DocumentId
		Integer id;

		@MarkerBinding(binder = @MarkerBinderRef(type = ParametricBinder.class,
				params = @Param(name = "stringScale", value = "3")))
		private Integer scale3Property;

		@MarkerBinding(binder = @MarkerBinderRef(type = ParametricBinder.class,
				params = @Param(name = "stringScale", value = "2")))
		private Integer scale2Property;
	}

	public static class NonAnnotatedEntity {
		Integer id;
		private Integer scale3Property;
		private Integer scale2Property;
	}

	private static void verifyExtractedModelType() {
		assertThat( extractedModelType ).isNotNull();
		assertThat( extractedModelType.property( "scale3Property" ).markers( ScaleMarker.class ) )
				.extracting( "scale" ).containsExactly( 3 );
		assertThat( extractedModelType.property( "scale2Property" ).markers( ScaleMarker.class ) )
				.extracting( "scale" ).containsExactly( 2 );
	}
}
