/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeReference;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JavaBeanAnnotationMappingRoutingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	private SearchMappingRepository mappingRepository;
	private JavaBeanMapping mapping;

	@Before
	public void setup() {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create( mappingRepositoryBuilder );

		initiator.annotationMapping().add( IndexedEntity.class );

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.explicitRouting()
				.field( "value", String.class )
		);

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = initiator.getResult();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( mappingRepository != null ) {
			mappingRepository.close();
		}
	}

	@Test
	public void index() {
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			manager.getMainWorker().add( entity1 );

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
		try ( PojoSearchManager manager = mapping.createSearchManagerWithOptions()
				.tenantId( "myTenantId" )
				.build() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setCategory( EntityCategory.CATEGORY_2 );
			entity1.setValue( "val1" );

			manager.getMainWorker().add( entity1 );

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
		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			SearchQuery<PojoReference> query = manager.search( IndexedEntity.class )
					.query()
					.asReferences()
					.predicate().match().onField( "value" ).matching( "val1" )
					.routing( "category_2" )
					.build();

			backendMock.expectSearchReferences(
					Collections.singletonList( IndexedEntity.INDEX ),
					b -> b.routingKey( "category_2" ),
					StubSearchWorkBehavior.empty()
			);

			query.execute();
			backendMock.verifyExpectationsMet();
		}
	}

	// TODO implement filters and allow them to use routing predicates, then test this here

	public enum EntityCategory {
		CATEGORY_1,
		CATEGORY_2;
	}

	@Indexed(index = IndexedEntity.INDEX)
	@MyRoutingKeyBridgeAnnotation
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

		@Field
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@RoutingKeyBridgeMapping(bridge = @RoutingKeyBridgeReference(type = MyRoutingKeyBridge.class))
	public @interface MyRoutingKeyBridgeAnnotation {
	}

	public static final class MyRoutingKeyBridge implements RoutingKeyBridge {

		private PojoModelElementAccessor<EntityCategory> categoryAccessor;

		@Override
		public void bind(PojoModelElement pojoModelElement) {
			categoryAccessor = pojoModelElement.property( "category" ).createAccessor( EntityCategory.class );
		}

		@Override
		public String toRoutingKey(String tenantIdentifier, Object entityIdentifier, PojoElement source) {
			EntityCategory category = categoryAccessor.read( source );
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
