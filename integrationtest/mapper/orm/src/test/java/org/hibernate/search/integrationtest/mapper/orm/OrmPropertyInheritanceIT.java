/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class OrmPropertyInheritanceIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "parentDeclaredProperty", String.class )
				.field( "childDeclaredProperty", String.class )
				.objectField( "embedded", b2 -> b2
						.field( "parentDeclaredProperty", String.class )
						// If property inheritance is not handled correctly, this field will be missing
						.field( "childDeclaredProperty", String.class )
				)
		);

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup(
						ParentIndexedEntity.class,
						IndexedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setParentDeclaredProperty( "parent-declared-1" );
			entity1.setChildDeclaredProperty( "child-declared-1" );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity1.setParentDeclaredProperty( "parent-declared-2" );
			entity1.setChildDeclaredProperty( "child-declared-2" );

			entity1.setEmbedded( entity2 );

			session.persist( entity2 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "2", b -> b
							.field( "parentDeclaredProperty", entity2.getParentDeclaredProperty() )
							.field( "childDeclaredProperty", entity2.getChildDeclaredProperty() )
					)
					.add( "1", b -> b
							.field( "parentDeclaredProperty", entity1.getParentDeclaredProperty() )
							.field( "childDeclaredProperty", entity1.getChildDeclaredProperty() )
							.objectField( "embedded", b2 -> b2
									.field( "parentDeclaredProperty", entity2.getParentDeclaredProperty() )
									.field( "childDeclaredProperty", entity2.getChildDeclaredProperty() )
							)
					)
					.preparedThenExecuted();
		} );
	}

	@Entity(name = "parentIndexedEntity")
	public abstract static class ParentIndexedEntity {

		private Integer id;

		private String parentDeclaredProperty;

		private List<ParentIndexedEntity> embedding = new ArrayList<>();

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Field
		public String getParentDeclaredProperty() {
			return parentDeclaredProperty;
		}

		public void setParentDeclaredProperty(String parentDeclaredProperty) {
			this.parentDeclaredProperty = parentDeclaredProperty;
		}

		@ManyToOne
		@IndexedEmbedded(maxDepth = 1)
		public abstract ParentIndexedEntity getEmbedded();

		// Not declaring the setter will make Hibernate Annotation Commons ignore the property, so we need this.
		public abstract void setEmbedded(ParentIndexedEntity parentIndexedEntity);

		@OneToMany(mappedBy = "embedded")
		public List<ParentIndexedEntity> getEmbedding() {
			return embedding;
		}

		public void setEmbedding(List<ParentIndexedEntity> embedding) {
			this.embedding = embedding;
		}
	}

	@Entity(name = "indexedEntity")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity extends ParentIndexedEntity {

		static final String INDEX = "IndexedEntity";

		private String childDeclaredProperty;

		private IndexedEntity embedded;

		@Field
		public String getChildDeclaredProperty() {
			return childDeclaredProperty;
		}

		public void setChildDeclaredProperty(String childDeclaredProperty) {
			this.childDeclaredProperty = childDeclaredProperty;
		}

		@Override
		public IndexedEntity getEmbedded() {
			return embedded;
		}

		@Override
		public void setEmbedded(ParentIndexedEntity embedded) {
			setEmbedded( (IndexedEntity) embedded );
		}

		public void setEmbedded(IndexedEntity embedded) {
			this.embedded = embedded;
		}
	}


}
