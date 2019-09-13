/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AnnotationMappingRoutingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.explicitRouting()
				.field( "value", String.class )
		);

		mapping = setupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			session.indexingPlan().add( entity1 );

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

			session.indexingPlan().add( entity1 );

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
			SearchQuery<EntityReference> query = session.search( IndexedEntity.class )
					.asEntityReference()
					.predicate( f -> f.match().field( "value" ).matching( "val1" ) )
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

	@Indexed(index = IndexedEntity.INDEX)
	@MyRoutingKeyBinding
	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private EntityCategory category;

		private String value;

		@DocumentId
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

		@GenericField
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@RoutingKeyBinding(binder = @RoutingKeyBinderRef(type = MyRoutingKeyBridge.Binder.class))
	public @interface MyRoutingKeyBinding {
	}

	public static final class MyRoutingKeyBridge implements RoutingKeyBridge {

		private final PojoElementAccessor<EntityCategory> categoryAccessor;

		private MyRoutingKeyBridge(PojoElementAccessor<EntityCategory> categoryAccessor) {
			this.categoryAccessor = categoryAccessor;
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

		public static class Binder implements RoutingKeyBinder<MyRoutingKeyBinding> {
			@Override
			public void bind(RoutingKeyBindingContext context) {
				PojoElementAccessor<EntityCategory> categoryAccessor =
						context.getBridgedElement().property( "category" )
								.createAccessor( EntityCategory.class );
				context.setBridge( new MyRoutingKeyBridge( categoryAccessor ) );
			}
		}
	}

}
