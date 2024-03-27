/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test adding default value bridges so that custom types are supported by the {@code @GenericField} annotation.
 */
@TestForIssue(jiraKey = "HSEARCH-3096")
class FieldDefaultBridgeAdditionIT {
	private static final String INDEX_NAME = "indexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void exactType() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.exactType( CustomClass.class )
				.valueBridge( new CustomClassBridge() );

		// Properties with the exact type will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassProperty.class,
				IndexedEntityWithCustomClassProperty::new );

		// Properties with a subtype won't match anything and will fail the boot
		doTestFailure( configurer, IndexedEntityWithCustomClassSubclass1Property.class );
		doTestFailure( configurer, IndexedEntityWithCustomClassSubclass2Property.class );
	}

	@Test
	void subTypesOf() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.subTypesOf( CustomClass.class )
				.valueBinder( bindingContext -> {
					Class<?> rawType = bindingContext.bridgedElement().rawType();
					if ( CustomClass.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClass.class, new CustomClassBridge() );
					}
					else if ( CustomClassSubclass1.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass1.class, new CustomClassSubclass1Bridge() );
					}
					else if ( CustomClassSubclass2.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass2.class, new CustomClassSubclass2Bridge() );
					}
				} );

		// Properties with the exact type will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassProperty.class,
				IndexedEntityWithCustomClassProperty::new );

		// Properties with a strict subtype will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass1Property.class,
				IndexedEntityWithCustomClassSubclass1Property::new );
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass2Property.class,
				IndexedEntityWithCustomClassSubclass2Property::new );
	}

	@Test
	void strictSubTypesOf() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.strictSubTypesOf( CustomClass.class )
				.valueBinder( bindingContext -> {
					Class<?> rawType = bindingContext.bridgedElement().rawType();
					if ( CustomClassSubclass1.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass1.class, new CustomClassSubclass1Bridge() );
					}
					else if ( CustomClassSubclass2.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass2.class, new CustomClassSubclass2Bridge() );
					}
				} );

		// Properties with the exact type won't match anything and will fail the boot
		doTestFailure( configurer, IndexedEntityWithCustomClassProperty.class );

		// Properties with a strict subtype will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass1Property.class,
				IndexedEntityWithCustomClassSubclass1Property::new );
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass2Property.class,
				IndexedEntityWithCustomClassSubclass2Property::new );
	}

	private <T> void doTestSuccess(Consumer<BridgesConfigurationContext> bridgesConfigurer,
			Class<T> indexedType, BiFunction<Integer, String, T> constructor) {
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "property", String.class ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> bridgesConfigurer.accept( builder.bridges() ) )
				.setup( indexedType );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			T entity1 = constructor.apply( 1, "value1" );
			T entity2 = constructor.apply( 2, "value2" );
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "property", "value1" ) )
					.add( "2", b -> b.field( "property", "value2" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	private <T> void doTestFailure(Consumer<BridgesConfigurationContext> bridgesConfigurer, Class<T> indexedType) {
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( builder -> bridgesConfigurer.accept( builder.bridges() ) )
				.setup( indexedType ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( indexedType.getName() )
						.pathContext( ".property" )
						.failure( "No default value bridge implementation for type",
								"Use a custom bridge" ) );
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassProperty {
		@DocumentId
		private Integer id;
		@GenericField
		private CustomClass property;

		public IndexedEntityWithCustomClassProperty(Integer id, String property) {
			this.id = id;
			this.property = new CustomClass( property );
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassSubclass1Property {
		@DocumentId
		private Integer id;
		@GenericField
		private CustomClassSubclass1 property;

		public IndexedEntityWithCustomClassSubclass1Property(Integer id, String property) {
			this.id = id;
			this.property = new CustomClassSubclass1( property );
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassSubclass2Property {
		@DocumentId
		private Integer id;
		@GenericField
		private CustomClassSubclass2 property;

		public IndexedEntityWithCustomClassSubclass2Property(Integer id, String property) {
			this.id = id;
			this.property = new CustomClassSubclass2( property );
		}
	}

	private static class CustomClass {
		final String innerValue;

		CustomClass(String innerValue) {
			this.innerValue = innerValue;
		}
	}

	private static class CustomClassSubclass1 extends CustomClass {
		CustomClassSubclass1(String innerValue) {
			super( innerValue );
		}
	}

	private static class CustomClassSubclass2 extends CustomClass {
		CustomClassSubclass2(String innerValue) {
			super( innerValue );
		}
	}

	private static class CustomClassBridge implements ValueBridge<CustomClass, String> {
		@Override
		public String toIndexedValue(CustomClass value, ValueBridgeToIndexedValueContext context) {
			return value.innerValue;
		}

		@Override
		public CustomClass fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
			return new CustomClass( value );
		}
	}

	private static class CustomClassSubclass1Bridge implements ValueBridge<CustomClassSubclass1, String> {
		@Override
		public String toIndexedValue(CustomClassSubclass1 value, ValueBridgeToIndexedValueContext context) {
			return value.innerValue;
		}

		@Override
		public CustomClassSubclass1 fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
			return new CustomClassSubclass1( value );
		}
	}

	private static class CustomClassSubclass2Bridge implements ValueBridge<CustomClassSubclass2, String> {
		@Override
		public String toIndexedValue(CustomClassSubclass2 value, ValueBridgeToIndexedValueContext context) {
			return value.innerValue;
		}

		@Override
		public CustomClassSubclass2 fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
			return new CustomClassSubclass2( value );
		}
	}

}
