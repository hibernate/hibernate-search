/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-1710")
class ContainedInTriggerUnnecessaryCollectionInitializationIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Group.INDEX );
		backendMock.expectAnySchema( Post.INDEX );

		sessionFactory = ormSetupHelper.start()
				.setup(
						Group.class,
						Post.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void test() {
		with( sessionFactory ).runInTransaction( session -> {
			Group group = new Group();
			group.setId( 1 );
			group.setSomeField( "initialValue" );
			group.setSomeInteger( 42 );

			Post post = new Post();
			post.setId( 2 );
			post.setGroup( group );
			group.getPosts().add( post );

			session.persist( group );
			session.persist( post );

			backendMock.expectWorks( Group.INDEX )
					.add( "1", b -> b
							.field( "someField", "initialValue" )
							.field( "someInteger", 42 )
					);

			backendMock.expectWorks( Post.INDEX )
					.add( "2", b -> b
							.objectField( "group", b2 -> b2
									.field( "someInteger", 42 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		AtomicReference<Group> groupFromModifyingTransaction = new AtomicReference<>();
		with( sessionFactory ).runInTransaction( session -> {
			Group group = session.getReference( Group.class, 1 );
			groupFromModifyingTransaction.set( group );

			// The posts should not be initialized
			assertThat( group.getPosts() )
					.isInstanceOf( PersistentCollection.class )
					.satisfies( p -> assertThat( Hibernate.isInitialized( p ) )
							.as( "The posts should not be initialized" )
							.isFalse()
					);

			group.setSomeField( "updatedValue" );

			backendMock.expectWorks( Group.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "someField", "updatedValue" )
							.field( "someInteger", 42 )
					);

			// Do not expect any work on the Post index
		} );
		backendMock.verifyExpectationsMet();

		Group group = groupFromModifyingTransaction.get();
		assertThat( group.getPosts() )
				.isInstanceOf( PersistentCollection.class )
				.satisfies( p -> assertThat( Hibernate.isInitialized( p ) )
						.as( "The posts should not be initialized by Hibernate Search" )
						.isFalse()
				);
	}

	@Entity(name = "Group_")
	@Indexed(index = Group.INDEX)
	public static class Group {
		public static final String INDEX = "Group";

		@Id
		private Integer id;

		@GenericField
		private String someField;

		@GenericField
		private int someInteger;

		@OneToMany(mappedBy = "group")
		@SuppressWarnings("deprecation")
		@org.hibernate.annotations.LazyCollection(org.hibernate.annotations.LazyCollectionOption.EXTRA)
		private Set<Post> posts = new HashSet<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getSomeField() {
			return someField;
		}

		public void setSomeField(String someField) {
			this.someField = someField;
		}

		public int isSomeInteger() {
			return someInteger;
		}

		public void setSomeInteger(int someInteger) {
			this.someInteger = someInteger;
		}

		public Set<Post> getPosts() {
			return posts;
		}
	}

	@Entity(name = "Post")
	@Indexed(index = Post.INDEX)
	public static class Post {
		public static final String INDEX = "Post";

		@Id
		private Integer id;

		@ManyToOne
		@IndexedEmbedded(includePaths = { "someInteger" })
		private Group group;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Group getGroup() {
			return group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}
	}
}
