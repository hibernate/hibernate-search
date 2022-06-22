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
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link TypeBinding} annotation.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3135")
public class TypeBindingBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type binding will be applied as expected.
	 */
	@Test
	public void simple() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( IndexedEntityWithWorkingTypeBinding.class );
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	@TypeBinding(binder = @TypeBinderRef(type = WorkingTypeBinder.class))
	private static class IndexedEntityWithWorkingTypeBinding {
		@DocumentId
		Integer id;
		String text;
	}

	public static class WorkingTypeBinder implements TypeBinder {
		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().use( "text" );
			IndexFieldReference<String> indexFieldReference =
					context.indexSchemaElement().field( "myText", f -> f.asString() )
							.toReference();
			context.bridge( IndexedEntityWithWorkingTypeBinding.class, (DocumentElement target,
					IndexedEntityWithWorkingTypeBinding bridgedElement, TypeBridgeWriteContext context1) -> {
				target.addValue(
						indexFieldReference, bridgedElement.text
				);
			} );
		}
	}

	@Test
	public void missingBinderReference() {
		@Indexed
		@TypeBinding(binder = @TypeBinderRef)
		class IndexedEntity {
			@DocumentId
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.annotationContextAnyParameters( TypeBinding.class )
						.failure( "Empty binder reference." )
				);
	}

	@Test
	public void customBridge_withParams_annotationMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
			b.field( "quotient", Integer.class );
			b.field( "reminder", Integer.class );
		} );
		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( AnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new AnnotatedEntity( 1, 14, 3 ) );
			plan.add( new AnnotatedEntity( 2, -7, 99 ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> {
						b.field( "quotient", 2 );
						b.field( "reminder", 3 );
					} )
					.add( "2", b -> {
						b.field( "quotient", 13 );
						b.field( "reminder", 1 );
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
						.annotationContextAnyParameters( TypeBinding.class )
						.failure( "Conflicting usage of @Param annotation for parameter name: 'stringBase'. " +
								"Can't assign both value '7' and '7'" )
				);
	}

	@Test
	public void customBridge_withParams_programmaticMapping() {
		backendMock.expectSchema( INDEX_NAME, b -> {
			b.field( "quotient", Integer.class );
			b.field( "reminder", Integer.class );
		} );
		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.withConfiguration( builder -> {
					builder.addEntityType( NotAnnotatedEntity.class );

					TypeMappingStep indexedEntity = builder.programmaticMapping().type( NotAnnotatedEntity.class )
							.binder( new ParametricBinder(), Collections.singletonMap( "base", 7 ) );
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "value1" );
					indexedEntity.property( "value2" );
				} )
				.setup( NotAnnotatedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan plan = session.indexingPlan();
			plan.add( new NotAnnotatedEntity( 1, 14, 3 ) );
			plan.add( new NotAnnotatedEntity( 2, -7, 99 ) );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> {
						b.field( "quotient", 2 );
						b.field( "reminder", 3 );
					} )
					.add( "2", b -> {
						b.field( "quotient", 13 );
						b.field( "reminder", 1 );
					} );
		}
		backendMock.verifyExpectationsMet();
	}

	public static class ParametricBinder implements TypeBinder {

		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().use( "value1" ).use( "value2" );

			IndexFieldReference<Integer> quotient = context.indexSchemaElement().field( "quotient", f -> f.asInteger() )
					.toReference();
			IndexFieldReference<Integer> reminder = context.indexSchemaElement().field( "reminder", f -> f.asInteger() )
					.toReference();
			Integer base = extractBase( context );

			context.bridge( (DocumentElement target, Object bridgedElement, TypeBridgeWriteContext writeContext) -> {
				if ( bridgedElement instanceof AnnotatedEntity ) {
					AnnotatedEntity casted = (AnnotatedEntity) bridgedElement;
					int sum = casted.value1 + casted.value2;
					target.addValue( quotient, sum / base );
					target.addValue( reminder, sum % base );
				}
				else if ( bridgedElement instanceof NotAnnotatedEntity ) {
					NotAnnotatedEntity casted = (NotAnnotatedEntity) bridgedElement;
					int sum = casted.value1 + casted.value2;
					target.addValue( quotient, sum / base );
					target.addValue( reminder, sum % base );
				}
			} );
		}
	}

	@SuppressWarnings("uncheked")
	private static Integer extractBase(TypeBindingContext context) {
		Optional<Object> optionalBase = context.paramOptional( "base" );
		if ( optionalBase.isPresent() ) {
			return (Integer) optionalBase.get();
		}

		String stringBase = (String) context.param( "stringBase" );
		return Integer.parseInt( stringBase );
	}

	@Indexed(index = INDEX_NAME)
	@TypeBinding(binder = @TypeBinderRef(type = ParametricBinder.class,
			params = @Param(name = "stringBase", value = "7")))
	private static class AnnotatedEntity {
		@DocumentId
		Integer id;

		int value1;
		int value2;

		AnnotatedEntity(Integer id, int value1, int value2) {
			this.id = id;
			this.value1 = value1;
			this.value2 = value2;
		}
	}

	@Indexed(index = INDEX_NAME)
	@TypeBinding(binder = @TypeBinderRef(type = ParametricBinder.class))
	private static class AnnotatedNoParamEntity {
		@DocumentId
		Integer id;

		int value1;
		int value2;

		AnnotatedNoParamEntity(Integer id, int value1, int value2) {
			this.id = id;
			this.value1 = value1;
			this.value2 = value2;
		}
	}

	@Indexed(index = INDEX_NAME)
	@TypeBinding(binder = @TypeBinderRef(type = ParametricBinder.class,
			params = { @Param(name = "stringBase", value = "7"), @Param(name = "stringBase", value = "7") }))
	private static class AnnotatedSameParamTwiceEntity {
		@DocumentId
		Integer id;

		int value1;
		int value2;

		AnnotatedSameParamTwiceEntity(Integer id, int value1, int value2) {
			this.id = id;
			this.value1 = value1;
			this.value2 = value2;
		}
	}

	private static class NotAnnotatedEntity {
		Integer id;
		int value1;
		int value2;

		NotAnnotatedEntity(Integer id, int value1, int value2) {
			this.id = id;
			this.value1 = value1;
			this.value2 = value2;
		}
	}
}
