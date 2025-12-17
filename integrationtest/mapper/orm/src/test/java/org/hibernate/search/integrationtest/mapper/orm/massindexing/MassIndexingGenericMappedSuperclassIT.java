/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * This test is similar to what we are doing in {@link MassIndexingBaseIT}.
 * The difference is in the entity definition.
 * We want to make sure that mass indexer has no issues creating ID queries for object with generics.
 * <p>
 * Related to <a href="https://hibernate.atlassian.net/browse/HHH-18007">HHH-18007</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingGenericMappedSuperclassIT {

	private SessionFactory sessionFactory;

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@SuppressWarnings("unused") // For EJC and lambda arg
	@BeforeEach
	void setup() {
		backendMock.resetExpectations();
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start().withPropertyRadical(
				HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.withAnnotatedTypes( Book.class );


		// We add the schema expectation as a part of a configuration, and as a last configuration.
		// this way we will only set the expectation only when the entire config was a success:
		setupContext.withConfiguration( ignored -> backendMock.expectAnySchema( Book.INDEX ) );
		sessionFactory = setupContext.setup();

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );
	}


	@Test
	void simple() throws Exception {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book extends BaseEntity<Integer> {

		public static final String INDEX = "Book";

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
			super( null );
		}

		public Book(Integer id, String title, String author) {
			super( id );
			this.title = title;
			this.author = author;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	@MappedSuperclass
	public abstract static class BaseEntity<ID extends Serializable> {
		@Id
		@DocumentId
		private ID id;

		protected BaseEntity(ID id) {
			this.id = id;
		}

		public ID getId() {
			return id;
		}
	}
}
