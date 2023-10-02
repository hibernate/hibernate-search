/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingDefaultCleanOperationIT {

	public static List<? extends Arguments> params() {
		List<Arguments> params = new ArrayList<>();

		for ( MassIndexingDefaultCleanOperation operation : MassIndexingDefaultCleanOperation.values() ) {
			params.add( Arguments.of( operation ) );
		}

		return params;
	}

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@ParameterizedTest
	@MethodSource("params")
	void test(MassIndexingDefaultCleanOperation operation) {
		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( StandalonePojoMapperSettings.Radicals.INDEXING_MASS_DEFAULT_CLEAN_OPERATION, operation )
				.setup( Book.class );


		initData();

		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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

			if ( MassIndexingDefaultCleanOperation.PURGE.equals( operation ) ) {
				// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
				// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
				backendMock.expectIndexScaleWorks( Book.NAME, searchSession.tenantIdentifierValue() )
						.purge()
						.mergeSegments()
						.flush()
						.refresh();
			}
			else {
				backendMock.expectSchemaManagementWorks( Book.NAME )
						.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

				// because we set dropAndCreateSchemaOnStart = true and do not explicitly set the purge value
				// it means that purge will default to false hence only flush and refresh are expected:
				backendMock.expectIndexScaleWorks( Book.NAME, searchSession.tenantIdentifierValue() )
						.flush()
						.refresh();
			}

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
	}

	private void persist(Book book) {
		loadingContext.persistenceMap( Book.PERSISTENCE_KEY ).put( book.id, book );
	}


	@SearchEntity(name = Book.NAME, loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
	public static class Book {

		public static final String NAME = "Book";
		public static final PersistenceTypeKey<Book, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Book.class, Integer.class );

		@DocumentId
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}
}
