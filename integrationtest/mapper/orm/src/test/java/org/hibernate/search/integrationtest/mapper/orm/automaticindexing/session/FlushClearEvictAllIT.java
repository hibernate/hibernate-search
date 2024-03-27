/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-1350")
class FlushClearEvictAllIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void before() {
		backendMock.expectAnySchema( Post.NAME );
		backendMock.expectAnySchema( Comment.NAME );
		entityManagerFactory = ormSetupHelper.start().setup( Post.class, Comment.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void test() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			Post post = new Post();
			post.setName( "This is a post" );

			EntityTransaction trx = entityManager.getTransaction();
			trx.begin();

			post = entityManager.merge( post );
			Long postId = post.getId();
			assertThat( postId ).isNotNull();

			backendMock.expectWorks( Post.NAME )
					.createFollowingWorks()
					.add( post.getId().toString(), b -> b.field( "name", "This is a post" ) );
			entityManager.flush();
			if ( ormSetupHelper.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			entityManager.clear();
			entityManagerFactory.getCache().evictAll();

			backendMock.expectWorks( Post.NAME )
					.executeFollowingWorks()
					.add( post.getId().toString(), b -> b.field( "name", "This is a post" ) );
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
					.createFollowingWorks()
					.add( "2", b -> b.field( "name", "This is a comment" ) );

			entityManager.flush();
			if ( ormSetupHelper.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			entityManager.clear();
			entityManagerFactory.getCache().evictAll();

			backendMock.expectWorks( Comment.NAME )
					.executeFollowingWorks()
					.add( "2", b -> b.field( "name", "This is a comment" ) );
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
		@SuppressWarnings("deprecation")
		@org.hibernate.annotations.LazyCollection(org.hibernate.annotations.LazyCollectionOption.EXTRA)
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
	@Table(name = "CommentTable") // Oracle 11g does not like Comment as table name
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
