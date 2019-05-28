/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.routing;

import java.util.Collections;

import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinitionContext;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class ProgrammaticMappingRoutingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper();

	private JavaBeanMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.explicitRouting()
				.field( "value", String.class )
		);

		mapping = setupHelper.withBackendMock( backendMock )
				.withConfiguration( builder -> {
					builder.addEntityType( IndexedEntity.class );

					ProgrammaticMappingDefinitionContext mappingDefinition = builder.programmaticMapping();
					mappingDefinition.type( IndexedEntity.class )
							.indexed( IndexedEntity.INDEX )
							.routingKeyBridge( MyRoutingKeyBridge.class )
							.property( "id" )
									.documentId()
							.property( "value" ).genericField();
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			session.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( b -> b
							.identifier( "1" )
							.routingKey( "category_2" )
							.document( StubDocumentNode.document()
									.field( "value", entity1.getValue() )
									.build()
							)
					)
					.preparedThenExecuted();
		}
	}

	@Test
	public void index_multiTenancy() {
		try ( SearchSession session = mapping.createSessionWithOptions()
				.tenantId( "myTenantId" )
				.build() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			session.getMainWorkPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( b -> b
							.identifier( "1" )
							.tenantIdentifier( "myTenantId" )
							.routingKey( "myTenantId/category_2" )
							.document( StubDocumentNode.document()
									.field( "value", entity1.getValue() )
									.build()
							)
					)
					.preparedThenExecuted();
		}
	}

	@Test
	public void search() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<PojoReference> query = session.search( IndexedEntity.class )
					.asReference()
					.predicate( f -> f.match().onField( "value" ).matching( "val1" ) )
					.routing( "category_2" )
					.toQuery();

			backendMock.expectSearchReferences(
					Collections.singletonList( IndexedEntity.INDEX ),
					b -> b.routingKey( "category_2" ),
					StubSearchWorkBehavior.empty()
			);

			query.fetch();
			backendMock.verifyExpectationsMet();
		}
	}

	// TODO HSEARCH-3325 implement filters and allow them to use routing predicates, then test this here

	public enum EntityCategory {
		CATEGORY_1,
		CATEGORY_2;
	}

	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private EntityCategory category;

		private String value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityCategory getCategory() {
			return category;
		}

		public void setCategory(EntityCategory category) {
			this.category = category;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public static final class MyRoutingKeyBridge implements RoutingKeyBridge {

		private PojoElementAccessor<EntityCategory> categoryAccessor;

		@Override
		public void bind(RoutingKeyBridgeBindingContext context) {
			categoryAccessor = context.getBridgedElement().property( "category" )
					.createAccessor( EntityCategory.class );
		}

		@Override
		public String toRoutingKey(String tenantIdentifier, Object entityIdentifier, Object bridgedElement,
				RoutingKeyBridgeToRoutingKeyContext context) {
			EntityCategory category = categoryAccessor.read( bridgedElement );
			StringBuilder keyBuilder = new StringBuilder();
			if ( tenantIdentifier != null ) {
				keyBuilder.append( tenantIdentifier ).append( "/" );
			}
			switch ( category ) {
				case CATEGORY_1:
					keyBuilder.append( "category_1" );
					break;
				case CATEGORY_2:
					keyBuilder.append( "category_2" );
					break;
				default:
					throw new RuntimeException( "Unknown category: " + category );
			}
			return keyBuilder.toString();
		}
	}

}
