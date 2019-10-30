/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

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
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class GenericFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void searchable() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity	{
			Integer id;
			Long searchable;
			LocalDate unsearchable;
			BigDecimal useDefault;
			String implicit;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(searchable = Searchable.YES)
			public Long getSearchable() {
				return searchable;
			}

			@GenericField(searchable = Searchable.NO)
			public LocalDate getUnsearchable() {
				return unsearchable;
			}

			@GenericField(searchable = Searchable.DEFAULT)
			public BigDecimal getUseDefault() {
				return useDefault;
			}

			@GenericField
			public String getImplicit() {
				return implicit;
			}
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
			Integer id;
			String enabled;
			String disabled;
			String explicitDefault;
			String implicitDefault;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(aggregable = Aggregable.YES)
			public String getEnabled() {
				return enabled;
			}

			@GenericField(aggregable = Aggregable.NO)
			public String getDisabled() {
				return disabled;
			}

			@GenericField(aggregable = Aggregable.DEFAULT)
			public String getExplicitDefault() {
				return explicitDefault;
			}

			@GenericField
			public String getImplicitDefault() {
				return implicitDefault;
			}
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(valueBridge = @ValueBridgeRef(type = ValidTypeBridge.class))
			public WrappedValue getWrap() {
				return wrap;
			}
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
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
			Integer id;
			WrappedValue wrap;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@GenericField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			public WrappedValue getWrap() {
				return wrap;
			}
		}

		SubTest.expectException(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.assertThrown()
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

	public static class ValidTypeBridge implements ValueBridge<WrappedValue, String> {
		@Override
		public String toIndexedValue(WrappedValue value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.wrapped;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.setBridge( WrappedValue.class, new ValidTypeBridge(), context.getTypeFactory().asString() );
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
				context.setBridge(
						WrappedValue.class, new InvalidTypeBridge(),
						context.getTypeFactory().extension( StubBackendExtension.get() ).asNonStandard( Integer.class )
				);
			}
		}
	}

	private static class WrappedValue {
		private String wrapped;
	}

}
