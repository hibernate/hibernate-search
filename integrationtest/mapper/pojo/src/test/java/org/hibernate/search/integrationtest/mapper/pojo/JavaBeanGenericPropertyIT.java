/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanGenericPropertyIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SearchMappingRepository mappingRepository;
	private JavaBeanMapping mapping;

	@Before
	public void setup() {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		contributor.annotationMapping().add( IndexedEntity.class ).add( GenericEntity.class );

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "genericProperty", b2 -> b2
						/*
						 * If generics are not handled correctly, these fields will have type "T" or "Object"
						 * and Hibernate Search will fail to resolve the bridge for them
						 */
						.field( "content", String.class )
						.field( "arrayContent", String.class )
				)
		);

		mappingRepository = mappingRepositoryBuilder.build();
		mapping = contributor.getResult();
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
			GenericEntity<String> genericEntity = new GenericEntity<>();
			genericEntity.setContent( "genericEntityContent" );
			genericEntity.setArrayContent( new String[] { "entry1", "entry2" } );

			entity1.setGenericProperty( genericEntity );

			manager.getMainWorker().add( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "genericProperty", b2 -> b2
									.field( "content", genericEntity.getContent() )
									.field( "arrayContent", genericEntity.getArrayContent()[0] )
									.field( "arrayContent", genericEntity.getArrayContent()[1] )
							)
					)
					.preparedThenExecuted();
		}
	}

	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		private Integer id;

		private GenericEntity<String> genericProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@IndexedEmbedded
		public GenericEntity<String> getGenericProperty() {
			return genericProperty;
		}

		public void setGenericProperty(GenericEntity<String> genericProperty) {
			this.genericProperty = genericProperty;
		}
	}

	public static class GenericEntity<T> {

		private T content;

		private T[] arrayContent;

		@Field
		public T getContent() {
			return content;
		}

		public void setContent(T content) {
			this.content = content;
		}

		@Field
		public T[] getArrayContent() {
			return arrayContent;
		}

		public void setArrayContent(T[] arrayContent) {
			this.arrayContent = arrayContent;
		}
	}

}
