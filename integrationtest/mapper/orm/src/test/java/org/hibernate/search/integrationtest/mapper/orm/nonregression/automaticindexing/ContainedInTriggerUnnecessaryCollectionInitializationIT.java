/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.nonregression.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1710")
public class ContainedInTriggerUnnecessaryCollectionInitializationIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
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
	public void test() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
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
					)
					.preparedThenExecuted();

			backendMock.expectWorks( Post.INDEX )
					.add( "2", b -> b
							.objectField( "group", b2 -> b2
									.field( "someInteger", 42 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		AtomicReference<Group> groupFromModifyingTransaction = new AtomicReference<>();
		OrmUtils.withinTransaction( sessionFactory, session -> {
			Group group = session.load( Group.class, 1 );
			groupFromModifyingTransaction.set( group );

			// The posts should not be initialized
			assertThat( group.getPosts() )
					.isInstanceOf( PersistentCollection.class )
					.satisfies( p -> assertFalse(
							"The posts should not be initialized",
							Hibernate.isInitialized( p )
					) );

			group.setSomeField( "updatedValue" );

			backendMock.expectWorks( Group.INDEX )
					.update( "1", b -> b
							.field( "someField", "updatedValue" )
							.field( "someInteger", 42 )
					)
					.preparedThenExecuted();

			// Do not expect any work on the Post index
		} );
		backendMock.verifyExpectationsMet();

		Group group = groupFromModifyingTransaction.get();
		assertThat( group.getPosts() )
				.isInstanceOf( PersistentCollection.class )
				.satisfies( p -> assertFalse(
						"The posts should not be initialized by Hibernate Search",
						Hibernate.isInitialized( p )
				) );
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
		@LazyCollection(LazyCollectionOption.EXTRA)
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
