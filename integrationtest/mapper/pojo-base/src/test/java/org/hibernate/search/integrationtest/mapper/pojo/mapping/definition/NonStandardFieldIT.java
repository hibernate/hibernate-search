/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendExtension;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link NonStandardField} annotation.
 * <p>
 * Does not test error cases common to all kinds of {@code @XXField} annotations, which are tested in {@link FieldBaseIT}.
 * <p>
 * Does not test default bridges, which are tested in {@link FieldDefaultBridgeBaseIT}.
 * <p>
 * Does not test uses of container value extractors, which are tested in {@link FieldContainerExtractorBaseIT}
 * (and others, see javadoc on that class).
 */
public class NonStandardFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@NonStandardField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", String.class )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@NonStandardField(name = "explicitName",
					valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", String.class )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name_invalid_dot() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@NonStandardField(name = "invalid.withdot", valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( NonStandardField.class )
						.failure( "Invalid index field name 'invalid.withdot': field names cannot contain a dot ('.')" ) );
	}

	@Test
	public void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@NonStandardField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void defaultBridge_invalidFieldType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@NonStandardField
			Integer myProperty;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".myProperty" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of non-standard type",
								"the resolved field type is standard",
								"Switch to a standard field annotation such as @GenericField",
								"encountered type DSL step '",
								"does extend the interface '" + StandardIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	public void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@NonStandardField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of non-standard type",
								"the resolved field type is standard",
								"Switch to a standard field annotation such as @GenericField",
								"encountered type DSL step '",
								"does extend the interface '" + StandardIndexFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@NonStandardField(valueBinder = @ValueBinderRef(type = GenericTypeBridge.Binder.class))
			String property;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".property" )
						.failure( "Unable to infer index field type for value bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter F to 'T'."
								+ " The index field type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the index field type explicitly,"
								+ " or set the type parameter F to a definite, raw type." ) );
	}

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge(
						WrappedValue.class, new ValidTypeBridge(),
						context.typeFactory().extension( StubBackendExtension.get() ).asNonStandard( String.class )
				);
			}
		}
	}

	public static class InvalidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge(
						WrappedValue.class, new InvalidTypeBridge(),
						context.typeFactory().asString()
				);
			}
		}
	}

	public static class GenericTypeBridge<T> implements ValueBridge<String, T> {
		private static final String TOSTRING = "<GenericTypeBridge toString() result>";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public T toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class Binder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( String.class, new GenericTypeBridge<>() );
			}
		}
	}

	private static class WrappedValue {
		private String wrapped;
	}

}
