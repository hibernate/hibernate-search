/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test adding default identifier bridges so that custom types are supported by the {@code @DocumentId} annotation.
 */
@TestForIssue(jiraKey = "HSEARCH-3096")
public class DocumentIdDefaultBridgeAdditionIT {
	private static final String INDEX_NAME = "indexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void exactType() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.exactType( CustomClass.class )
				.identifierBridge( new CustomClassBridge() );

		// Properties with the exact type will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassId.class,
				IndexedEntityWithCustomClassId::new );

		// Properties with a subtype won't match anything and will fail the boot
		doTestFailure( configurer, IndexedEntityWithCustomClassSubclass1Id.class );
	}

	@Test
	public void subTypesOf() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.subTypesOf( CustomClass.class )
				.identifierBinder( bindingContext -> {
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
		doTestSuccess( configurer, IndexedEntityWithCustomClassId.class,
				IndexedEntityWithCustomClassId::new );

		// Properties with a strict subtype will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass1Id.class,
				IndexedEntityWithCustomClassSubclass1Id::new );
	}

	@Test
	public void strictSubTypesOf() {
		Consumer<BridgesConfigurationContext> configurer = b -> b.strictSubTypesOf( CustomClass.class )
				.identifierBinder( bindingContext -> {
					Class<?> rawType = bindingContext.bridgedElement().rawType();
					if ( CustomClassSubclass1.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass1.class, new CustomClassSubclass1Bridge() );
					}
					else if ( CustomClassSubclass2.class.equals( rawType ) ) {
						bindingContext.bridge( CustomClassSubclass2.class, new CustomClassSubclass2Bridge() );
					}
				} );

		// Properties with the exact type won't match anything and will fail the boot
		doTestFailure( configurer, IndexedEntityWithCustomClassId.class );

		// Properties with a strict subtype will match and will be assigned our default binder
		doTestSuccess( configurer, IndexedEntityWithCustomClassSubclass1Id.class,
				IndexedEntityWithCustomClassSubclass1Id::new );
	}

	private <T> void doTestSuccess(Consumer<BridgesConfigurationContext> bridgesConfigurer,
			Class<T> indexedType, Function<String, T> constructor) {
		backendMock.expectSchema( INDEX_NAME, b -> { } );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> bridgesConfigurer.accept( builder.bridges() ) )
				.setup( indexedType );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			T entity1 = constructor.apply( "id1" );
			T entity2 = constructor.apply( "id2" );
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "id1", b -> { } )
					.add( "id2", b -> { } );
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
						.pathContext( ".id" )
						.failure( "No default identifier bridge implementation for type", "Use a custom bridge" ) );
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassId {
		@DocumentId
		private CustomClass id;

		public IndexedEntityWithCustomClassId(String id) {
			this.id = new CustomClass( id );
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassSubclass1Id {
		@DocumentId
		private CustomClassSubclass1 id;

		public IndexedEntityWithCustomClassSubclass1Id(String id) {
			this.id = new CustomClassSubclass1( id );
		}
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithCustomClassSubclass2Id {
		@DocumentId
		private CustomClassSubclass2 id;

		public IndexedEntityWithCustomClassSubclass2Id(String id) {
			this.id = new CustomClassSubclass2( id );
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

	private static class CustomClassBridge implements IdentifierBridge<CustomClass> {
		@Override
		public String toDocumentIdentifier(CustomClass propertyValue,
				IdentifierBridgeToDocumentIdentifierContext context) {
			return propertyValue.innerValue;
		}

		@Override
		public CustomClass fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			return new CustomClass( documentIdentifier );
		}
	}

	private static class CustomClassSubclass1Bridge implements IdentifierBridge<CustomClassSubclass1> {
		@Override
		public String toDocumentIdentifier(CustomClassSubclass1 propertyValue,
				IdentifierBridgeToDocumentIdentifierContext context) {
			return propertyValue.innerValue;
		}

		@Override
		public CustomClassSubclass1 fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			return new CustomClassSubclass1( documentIdentifier );
		}
	}

	private static class CustomClassSubclass2Bridge implements IdentifierBridge<CustomClassSubclass2> {
		@Override
		public String toDocumentIdentifier(CustomClassSubclass2 propertyValue,
				IdentifierBridgeToDocumentIdentifierContext context) {
			return propertyValue.innerValue;
		}

		@Override
		public CustomClassSubclass2 fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			return new CustomClassSubclass2( documentIdentifier );
		}
	}

}
