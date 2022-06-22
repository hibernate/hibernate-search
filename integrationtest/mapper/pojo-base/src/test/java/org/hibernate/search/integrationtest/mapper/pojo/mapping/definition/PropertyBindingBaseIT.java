/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link PropertyBinding} annotation.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3135")
public class PropertyBindingBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type binding will be applied as expected.
	 */
	@Test
	public void simple() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( IndexedEntityWithWorkingPropertyBinding.class );
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithWorkingPropertyBinding {
		@DocumentId
		Integer id;
		@PropertyBinding(binder = @PropertyBinderRef(type = WorkingPropertyBinder.class))
		String text;
	}

	public static class WorkingPropertyBinder implements PropertyBinder {
		@Override
		public void bind(PropertyBindingContext context) {
			context.dependencies().useRootOnly();
			IndexFieldReference<String> indexFieldReference =
					context.indexSchemaElement().field( "myText", f -> f.asString() )
							.toReference();
			context.bridge( String.class,
					(DocumentElement target, String bridgedElement,
							PropertyBridgeWriteContext context1) -> {
						target.addValue( indexFieldReference, bridgedElement );
					} );
		}
	}

	@Test
	public void missingBinderReference() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@PropertyBinding(binder = @PropertyBinderRef)
			String text;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".text" )
						.annotationContextAnyParameters( PropertyBinding.class )
						.failure( "Empty binder reference." )
				);
	}

	@Test
	public void customBridge_withParams_annotationMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
			b.field( "sum", Integer.class );
			b.field( "diff", Integer.class );
		} );
		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( AnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new AnnotatedEntity( 1, 14 ) );
			plan.add( new AnnotatedEntity( 2, -7 ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> {
						b.field( "sum", 21 );
						b.field( "diff", 7 );
					} )
					.add( "2", b -> {
						b.field( "sum", 0 );
						b.field( "diff", -14 );
					} );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_withParams_paramNotDefined() {
		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( AnnotatedNoParamEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( AnnotatedNoParamEntity.class.getName() )
						.pathContext( ".value" )
						.failure( "Param with name 'stringBase' has not been defined for the binder." )
				);
	}

	@Test
	public void customBridge_withParams_paramDefinedTwice() {
		assertThatThrownBy(
				() -> setupHelper.start().expectCustomBeans().setup( AnnotatedSameParamTwiceEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( AnnotatedSameParamTwiceEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( PropertyBinding.class )
						.failure( "Conflicting usage of @Param annotation for parameter name: 'stringBase'. " +
								"Can't assign both value '7' and '7'" )
				);
	}

	@Test
	public void customBridge_withParams_programmaticMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
			b.field( "sum", Integer.class );
			b.field( "diff", Integer.class );
		} );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					builder.addEntityType( NotAnnotatedEntity.class );

					TypeMappingStep indexedEntity = builder.programmaticMapping().type( NotAnnotatedEntity.class );
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "value" ).binder( new ParametricBinder(),
							Collections.singletonMap( "base", 7 )
					);
				} )
				.expectCustomBeans().setup( NotAnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new NotAnnotatedEntity( 1, 14 ) );
			plan.add( new NotAnnotatedEntity( 2, -7 ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> {
						b.field( "sum", 21 );
						b.field( "diff", 7 );
					} )
					.add( "2", b -> {
						b.field( "sum", 0 );
						b.field( "diff", -14 );
					} );
		}
		backendMock.verifyExpectationsMet();
	}

	public static class ParametricBinder implements PropertyBinder {

		@Override
		public void bind(PropertyBindingContext context) {
			context.dependencies().useRootOnly();

			IndexFieldReference<Integer> sum = context.indexSchemaElement().field( "sum", f -> f.asInteger() )
					.toReference();
			IndexFieldReference<Integer> diff = context.indexSchemaElement().field( "diff", f -> f.asInteger() )
					.toReference();
			int base = extractBase( context );

			context.bridge( Integer.class, (DocumentElement target, Integer bridgedElement,
					PropertyBridgeWriteContext writeContext) -> {
				target.addValue( sum, bridgedElement + base );
				target.addValue( diff, bridgedElement - base );
			} );
		}

		@SuppressWarnings("uncheked")
		private static int extractBase(PropertyBindingContext context) {
			Optional<Object> optionalBase = context.paramOptional( "base" );
			if ( optionalBase.isPresent() ) {
				return (Integer) optionalBase.get();
			}

			String stringBase = (String) context.param( "stringBase" );
			return Integer.parseInt( stringBase );
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class AnnotatedEntity {
		@DocumentId
		Integer id;

		@PropertyBinding(binder = @PropertyBinderRef(type = ParametricBinder.class,
				params = @Param(name = "stringBase", value = "7")))
		int value;

		AnnotatedEntity(Integer id, int value) {
			this.id = id;
			this.value = value;
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class AnnotatedNoParamEntity {
		@DocumentId
		Integer id;

		@PropertyBinding(binder = @PropertyBinderRef(type = ParametricBinder.class))
		int value;

		AnnotatedNoParamEntity(Integer id, int value) {
			this.id = id;
			this.value = value;
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class AnnotatedSameParamTwiceEntity {
		@DocumentId
		Integer id;

		@PropertyBinding(binder = @PropertyBinderRef(type = ParametricBinder.class,
				params = { @Param(name = "stringBase", value = "7"), @Param(name = "stringBase", value = "7") }))
		int value;

		AnnotatedSameParamTwiceEntity(Integer id, int value) {
			this.id = id;
			this.value = value;
		}
	}

	private static class NotAnnotatedEntity {
		Integer id;
		int value;

		NotAnnotatedEntity(Integer id, int value) {
			this.id = id;
			this.value = value;
		}
	}
}
