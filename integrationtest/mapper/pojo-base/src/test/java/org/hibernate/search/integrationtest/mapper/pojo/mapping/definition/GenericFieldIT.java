/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendExtension;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class GenericFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@GenericField
			Long value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "value", Long.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@GenericField(name = "explicitName")
			LocalDate value;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", LocalDate.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void name_invalid_dot() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@GenericField(name = "invalid.withdot")
			Long value;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".value" )
						.annotationContextAnyParameters( GenericField.class )
						.failure(
								"Index field name 'invalid.withdot' is invalid: field names cannot contain a dot ('.')"
						)
						.build()
				);
	}

	@Test
	public void searchable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@GenericField(searchable = Searchable.YES)
			Long searchable;
			@GenericField(searchable = Searchable.NO)
			LocalDate unsearchable;
			@GenericField(searchable = Searchable.DEFAULT)
			BigDecimal useDefault;
			@GenericField
			String implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchable", Long.class, f -> f.searchable( Searchable.YES ) )
				.field( "unsearchable", LocalDate.class, f -> f.searchable( Searchable.NO ) )
				.field( "useDefault", BigDecimal.class )
				.field( "implicit", String.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void aggregable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			@DocumentId
			Integer id;
			@GenericField(aggregable = Aggregable.YES)
			String enabled;
			@GenericField(aggregable = Aggregable.NO)
			String disabled;
			@GenericField(aggregable = Aggregable.DEFAULT)
			String explicitDefault;
			@GenericField
			String implicitDefault;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "enabled", String.class, f -> f.aggregable( Aggregable.YES ) )
				.field( "disabled", String.class, f -> f.aggregable( Aggregable.NO ) )
				.field( "explicitDefault", String.class )
				.field( "implicitDefault", String.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_implicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "wrap", String.class )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			WrappedValue wrap;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".wrap" )
						.failure(
								"This property's mapping expects a standard type for the index field",
								"but the assigned value bridge or value binder declares a non-standard type",
								"encountered type DSL step '",
								"expected '" + StandardIndexFieldTypeOptionsStep.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3243")
	public void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
			String property;
		}

		assertThatThrownBy( () -> setupHelper.start().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".property" )
						.failure( "Bridge '" + GenericTypeBridge.TOSTRING + "' implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter F to 'T'."
								+ " The field type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the field type explicitly,"
								+ " or set the type parameter F to a definite, raw type." )
						.build() );
	}

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( WrappedValue.class, new ValidTypeBridge(), context.typeFactory().asString() );
			}
		}
	}

	public static class InvalidTypeBridge implements ValueBridge<WrappedValue, Integer> {
		@Override
		public Integer toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge(
						WrappedValue.class, new InvalidTypeBridge(),
						context.typeFactory().extension( StubBackendExtension.get() ).asNonStandard( Integer.class )
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
	}

	private static class WrappedValue {
		private String wrapped;
	}

}
