/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.util.common.rule.BackendMock;
import org.hibernate.search.integrationtest.util.common.rule.StaticCounters;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.integrationtest.util.orm.OrmUtils;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class OrmGenericPropertyIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( GenericEntity.class )
				.addAnnotatedClass( StringGenericEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

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

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void index() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			GenericEntity<String> genericEntity = new StringGenericEntity();
			genericEntity.setId( 2 );
			genericEntity.setContent( "genericEntityContent" );
			genericEntity.setArrayContent( new String[] { "entry1", "entry2" } );

			entity1.setGenericProperty( genericEntity );

			session.persist( genericEntity );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "genericProperty", b2 -> b2
									.field( "content", genericEntity.getContent() )
									.field( "arrayContent", genericEntity.getArrayContent()[0] )
									.field( "arrayContent", genericEntity.getArrayContent()[1] )
							)
					)
					.preparedThenExecuted();
		} );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		@DocumentId
		private Integer id;

		@ManyToOne
		private GenericEntity<String> genericProperty;

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

	@Entity(name = "generic")
	public abstract static class GenericEntity<T> {

		@Id
		@DocumentId
		private Integer id;

		@Basic
		@Type(type = "serializable") // Necessary for Hibernate ORM...
		@Field
		private T content;

		@Basic
		@Type(type = "serializable") // Necessary for Hibernate ORM...
		@Field
		private T[] arrayContent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public T getContent() {
			return content;
		}

		public void setContent(T content) {
			this.content = content;
		}

		public T[] getArrayContent() {
			return arrayContent;
		}

		public void setArrayContent(T[] arrayContent) {
			this.arrayContent = arrayContent;
		}
	}

	@Entity(name = "stringgeneric")
	public static class StringGenericEntity extends GenericEntity<String> {

	}

}
