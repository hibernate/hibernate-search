/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PropertyInheritanceIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "parentDeclaredProperty", String.class )
				.field( "childDeclaredProperty", String.class )
				.objectField( "embedded", b2 -> b2
						.field( "parentDeclaredProperty", String.class )
						// If property inheritance is not handled correctly, this field will be missing
						.field( "childDeclaredProperty", String.class )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						ParentIndexedEntity.class,
						IndexedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
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
					);
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

		@GenericField
		public String getParentDeclaredProperty() {
			return parentDeclaredProperty;
		}

		public void setParentDeclaredProperty(String parentDeclaredProperty) {
			this.parentDeclaredProperty = parentDeclaredProperty;
		}

		@ManyToOne
		@IndexedEmbedded(includeDepth = 1)
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

		@GenericField
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
