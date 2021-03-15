/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static org.assertj.core.api.Fail.fail;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Test mass indexing of an entity type whose document ID is not the entity ID.
 */
public class MassIndexingNonEntityIdDocumentIdIT {

	private static final String TITLE_1 = "Oliver Twist";
	private static final String AUTHOR_1 = "Charles Dickens";
	private static final String TITLE_2 = "Ulysses";
	private static final String AUTHOR_2 = "James Joyce";
	private static final String TITLE_3 = "Frankenstein";
	private static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private SearchMapping mapping;

	private Map<Integer, Book> booksmap = new LinkedHashMap<>();

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		mapping = setupHelper.start()
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void defaultMassIndexerStartAndWait() {
		try ( SearchSession searchSession = createSessionFromMap() ) {
			MassIndexer indexer = searchSession.massIndexer().mergeSegmentsOnFinish( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "41", b -> b
					.field( "title", TITLE_1 )
					.field( "author", AUTHOR_1 )
					)
					.add( "42", b -> b
					.field( "title", TITLE_2 )
					.field( "author", AUTHOR_2 )
					)
					.add( "43", b -> b
					.field( "title", TITLE_3 )
					.field( "author", AUTHOR_3 )
					)
					.createdThenExecuted();

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	private SearchSession createSessionFromMap() {
		return mapping.createSessionWithOptions().loading( (o) -> {
			o.registerLoader( Book.class, (identifiers) -> {
				return identifiers.stream()
						.map( (identifier) -> booksmap.get( (Integer) identifier ) )
						.collect( Collectors.toList() );
			} );

			o.massIndexingLoadingStrategy( Book.class, JavaBeanIndexingStrategies.from( booksmap ) );
		} ).build();
	}

	private void initData() {
		persist( new Book( 1, 41, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, 42, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, 43, TITLE_3, AUTHOR_3 ) );
	}

	private void persist(Book book) {
		booksmap.put( book.documentId, book );
	}

	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		private Integer id;

		@DocumentId
		private Integer documentId;

		@GenericField
		private String title;

		@GenericField
		private String author;

		protected Book() {
		}

		Book(int id, int documentId, String title, String author) {
			this.id = id;
			this.documentId = documentId;
			this.title = title;
			this.author = author;
		}
	}
}
