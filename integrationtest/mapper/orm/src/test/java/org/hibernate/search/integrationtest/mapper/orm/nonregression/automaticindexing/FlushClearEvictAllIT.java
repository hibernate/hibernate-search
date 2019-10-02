/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.nonregression.automaticindexing;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1350")
public class FlushClearEvictAllIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( Post.NAME );
		backendMock.expectAnySchema( Comment.NAME );
		sessionFactory = ormSetupHelper.start().setup( Post.class, Comment.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		OrmUtils.withinEntityManager( sessionFactory, entityManager -> {
			Post post = new Post();
			post.setName( "This is a post" );

			EntityTransaction trx = entityManager.getTransaction();
			trx.begin();

			post = entityManager.merge( post );
			Long postId = post.getId();
			assertNotNull( postId );

			backendMock.expectWorks( Post.NAME )
					.add( post.getId().toString(), b -> b.field( "name", "This is a post" ) )
					.processed();
			entityManager.flush();
			backendMock.verifyExpectationsMet();

			entityManager.clear();
			sessionFactory.getCache().evictAll();

			backendMock.expectWorks( Post.NAME )
					.add( post.getId().toString(), b -> b.field( "name", "This is a post" ) )
					.executed();
			trx.commit();
			backendMock.verifyExpectationsMet();

			trx.begin();

			Post reloaded = entityManager.find( Post.class, postId );
			Comment comment = new Comment();
			comment.setId( 2L );
			comment.setName( "This is a comment" );
			comment.setPost( reloaded );
			reloaded.getComments().add( comment );

			backendMock.expectWorks( Comment.NAME )
					.add( "2", b -> b.field( "name", "This is a comment" ) )
					.processed();
			entityManager.flush();
			backendMock.verifyExpectationsMet();

			entityManager.clear();
			sessionFactory.getCache().evictAll();

			backendMock.expectWorks( Comment.NAME )
					.add( "2", b -> b.field( "name", "This is a comment" ) )
					.executed();
			trx.commit();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Entity(name = Post.NAME)
	@Indexed(index = Post.NAME)
	public static class Post {
		public static final String NAME = "Post";

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@GenericField
		private String name;

		@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@LazyCollection(LazyCollectionOption.EXTRA)
		@OrderColumn(name = "idx")
		private List<Comment> comments = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Comment> getComments() {
			return comments;
		}
	}

	@Entity(name = Comment.NAME)
	@Indexed(index = Comment.NAME)
	public static class Comment {
		public static final String NAME = "Comment";

		@Id
		private Long id;

		@GenericField
		private String name;

		@ManyToOne(optional = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "FK_PostId", nullable = true, insertable = true, updatable = false)
		private Post post;

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}
	}
}
