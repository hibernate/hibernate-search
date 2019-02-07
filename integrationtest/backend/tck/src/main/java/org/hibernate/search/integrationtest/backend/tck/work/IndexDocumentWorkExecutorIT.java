/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;

/**
 * Verify that the {@link IndexDocumentWorkExecutor}, provided by a backend, is working properly, storing correctly the indexes.
 */
public class IndexDocumentWorkExecutorIT {

	private static final String INDEX_NAME = "lordOfTheRingsChapters";
	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void before() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void checkAllDocumentsAreSearchable() {
		IndexDocumentWorkExecutor<? extends DocumentElement> documentWorkExecutor = indexManager.createDocumentWorkExecutor();
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[NUMBER_OF_BOOKS];
		IndexWorkExecutor workExecutor = indexManager.createWorkExecutor();

		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = i + "";
			tasks[i] = documentWorkExecutor.add( referenceProvider( id ), document -> {
				indexAccessors.title.write( document, "The Lord of the Rings cap. " + id );
			} );
		}
		CompletableFuture.allOf( tasks ).join();
		workExecutor.flush().join();

		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();

		Assertions.assertThat( query.executeCount() ).isEqualTo( NUMBER_OF_BOOKS );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> title;

		IndexAccessors(IndexSchemaElement root) {
			title = root.field( "title", f -> f.asString() ).createAccessor();
		}
	}
}
