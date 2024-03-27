/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that Hibernate Search correctly unproxies entities before accessing entity fields to populate document,
 * so as to avoid fetching data from a private field on a proxy,
 * which would never work correctly as those private fields are always null on proxies.
 */
@TestForIssue(jiraKey = "HSEARCH-3643")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndexingProcessorProxiedAssociatedEntityIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );

		sessionFactory = ormSetupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class, ContainedEntity.class )
				.dataClearing( config -> config.clearOrder( ContainedEntity.class, IndexedEntity.class )
						.preClear( IndexedEntity.class, entity -> {
							entity.containedSingle = null;
							entity.containedList = new ArrayList<>();
						} ) )
				.setup();
	}

	@Test
	void toOne() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed1 = new IndexedEntity( 1, "initialValue" );

			ContainedEntity contained1 = new ContainedEntity( 2, "initialValue" );
			indexed1.setContainedSingle( contained1 );
			contained1.setContainingAsSingle( indexed1 );

			session.persist( contained1 );
			session.persist( indexed1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue" )
							.objectField( "containedSingle", b2 -> b2
									.field( "text", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed1 = session.getReference( IndexedEntity.class, 1 );

			// The contained entity should be a proxy, otherwise the test doesn't make sense
			assertThat( indexed1.getContainedSingle() ).isInstanceOf( HibernateProxy.class );

			indexed1.setText( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "updatedValue" )
							.objectField( "containedSingle", b2 -> b2
									.field( "text", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void toMany() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed1 = new IndexedEntity( 1, "initialValue" );

			ContainedEntity contained1 = new ContainedEntity( 2, "initialValue1" );
			indexed1.getContainedList().add( contained1 );
			contained1.setContainingAsList( indexed1 );

			session.persist( contained1 );
			session.persist( indexed1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue" )
							.objectField( "containedList", b2 -> b2
									.field( "text", "initialValue1" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			// Create a proxy for contained1, so that the "containedList" list in indexed1 is populated with that proxy.
			// The proxy will be initialized, but that's irrelevant to our test.
			@SuppressWarnings("unused") // Keep a reference to the proxy so that it's not garbage collected, which would prevent the above from happening.
			ContainedEntity contained1 = session.getReference( ContainedEntity.class, 2 );

			IndexedEntity indexed1 = session.getReference( IndexedEntity.class, 1 );

			// The new contained entity should be a proxy, otherwise the test doesn't make sense
			assertThat( indexed1.getContainedList().get( 0 ) ).isInstanceOf( HibernateProxy.class );

			indexed1.setText( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "updatedValue" )
							.objectField( "containedList", b2 -> b2
									.field( "text", "initialValue1" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {
		public static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		// This field is only used to trigger reindexing, we don't really care about its value
		@GenericField
		private String text;

		@OneToOne(fetch = FetchType.LAZY)
		@IndexedEmbedded
		private ContainedEntity containedSingle;

		@OneToMany(mappedBy = "containingAsList", fetch = FetchType.LAZY)
		@IndexedEmbedded
		private List<ContainedEntity> containedList = new ArrayList<>();

		protected IndexedEntity() {
			// For ORM
		}

		public IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public List<ContainedEntity> getContainedList() {
			return containedList;
		}

		public void setContainedList(List<ContainedEntity> containedList) {
			this.containedList = containedList;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	@Access(AccessType.FIELD) // This should be the default, but let's be safe: the test only makes sense with this access type
	public static class ContainedEntity {
		public static final String NAME = "ContainedEntity";

		@Id
		private Integer id;

		// This field is accessed directly (not through the getter), so it requires special handling if the entity is proxified
		@GenericField
		private String text;

		@OneToOne(mappedBy = "containedSingle")
		private IndexedEntity containingAsSingle;

		@ManyToOne
		private IndexedEntity containingAsList;

		protected ContainedEntity() {
			// For ORM
		}

		public ContainedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getContainingAsSingle() {
			return containingAsSingle;
		}

		public void setContainingAsSingle(IndexedEntity containingAsSingle) {
			this.containingAsSingle = containingAsSingle;
		}

		public IndexedEntity getContainingAsList() {
			return containingAsList;
		}

		public void setContainingAsList(IndexedEntity containingAsList) {
			this.containingAsList = containingAsList;
		}
	}
}
