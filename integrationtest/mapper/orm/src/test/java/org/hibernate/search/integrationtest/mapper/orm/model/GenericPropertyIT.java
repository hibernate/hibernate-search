/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class GenericPropertyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "genericProperty", b2 -> b2
						/*
						 * If generics are not handled correctly, these fields will have type "T" or "Object"
						 * and Hibernate Search will fail to resolve the bridge for them
						 */
						.field( "content", String.class )
						.field( "arrayContent", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						IndexedEntity.class,
						GenericEntity.class,
						StringGenericEntity.class
				);
		backendMock.verifyExpectationsMet();
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
			genericEntity.getContainingEntities().add( entity1 );

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
					.processedThenExecuted();
		} );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
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
		private Integer id;

		@Basic
		@Type(type = "serializable") // Necessary for Hibernate ORM...
		@GenericField
		private T content;

		@Basic
		@Type(type = "serializable") // Necessary for Hibernate ORM...
		@GenericField
		private T[] arrayContent;

		@OneToMany(mappedBy = "genericProperty")
		private List<IndexedEntity> containingEntities = new ArrayList<>();

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

		public List<IndexedEntity> getContainingEntities() {
			return containingEntities;
		}
	}

	@Entity(name = "stringgeneric")
	public static class StringGenericEntity extends GenericEntity<String> {

	}

}
