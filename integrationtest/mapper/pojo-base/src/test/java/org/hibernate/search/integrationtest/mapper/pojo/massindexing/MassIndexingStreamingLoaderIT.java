/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

public class MassIndexingStreamingLoaderIT {

	private static final int COUNT = 1500;

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public ThreadSpy threadSpy = ThreadSpy.create();

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void entityLoading() throws InterruptedException {

		SearchMapping mapping = setupEntityLoading();

		backendMock.expectIndexScaleWorks( Book.NAME )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();

		expectIndexingWorks();

		// additional 10 entities are added to total on each id-batch loaded:
		logged.expectEvent( Level.INFO, "Mass indexing is going to index 10 more entities" );

		try {
			mapping.scope( Object.class ).massIndexer()
					.threadsToLoadObjects( 1 )
					.batchSizeToLoadObjects( 10 )
					.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}
	}

	private void expectIndexingWorks() {
		BackendMock.DocumentWorkCallListContext expected = backendMock.expectWorks(
				Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);
		for ( int i = 0; i < COUNT; i++ ) {
			int id = i;
			expected.add( Integer.toString( id ), b -> b
					.field( "title", "title_" + id )
					.field( "author", "author_" + id )
			);
		}
	}

	private SearchMapping setupEntityLoading() {
		return setup( new MassLoadingStrategy<Book, Integer>() {
			@Override
			public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<Book> includedTypes,
					MassIdentifierSink<Integer> sink, MassLoadingOptions options) {
				return new MassIdentifierLoader() {
					private int i = 0;

					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public void loadNext() throws InterruptedException {
						sink.accept( IntStream.range( i, i = i + options.batchSize() )
								.boxed()
								.collect( Collectors.toList() ) );
						if ( i >= COUNT ) {
							sink.complete();
						}
					}
				};
			}

			@Override
			public MassEntityLoader<Integer> createEntityLoader(LoadingTypeGroup<Book> includedTypes,
					MassEntitySink<Book> sink, MassLoadingOptions options) {
				return new MassEntityLoader<Integer>() {
					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public void load(List<Integer> identifiers) throws InterruptedException {
						sink.accept( identifiers.stream()
								.map( i -> new Book( i, "title_" + i, "author_" + i ) )
								.collect( Collectors.toList() ) );
					}
				};
			}
		} );
	}

	private SearchMapping setup(MassLoadingStrategy<Book, Integer> loadingStrategy) {
		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.withConfiguration( b -> b.programmaticMapping().type( Book.class )
						.searchEntity()
						.loadingBinder( (EntityLoadingBinder) ctx -> ctx.massLoadingStrategy( Book.class, loadingStrategy ) ) )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		return mapping;
	}

	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";
		public static final PersistenceTypeKey<Book, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Book.class, Integer.class );

		private Integer id;

		private String title;

		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@GenericField
		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}
}
