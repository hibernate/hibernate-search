/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class LuceneIndexSchemaManagerDropAndCreateIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void test() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		dropAndCreate();

		assertThat( indexExists() ).isTrue();

		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {
		} );
		plan.execute().join();

		assertThat( countDocsOnDisk() ).isEqualTo( 1 );

		dropAndCreate();

		assertThat( indexExists() ).isTrue();
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, INDEX_NAME );
	}

	private int countDocsOnDisk() throws IOException {
		return LuceneIndexContentUtils.readIndex(
				setupHelper, INDEX_NAME,
				reader -> reader.getDocCount( MetadataFields.idFieldName() )
		);
	}

	private void dropAndCreate() {
		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.DROP_AND_CREATE.apply( indexManager.getSchemaManager() )
		);
	}

	private void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "field", f -> f.asString() )
									.toReference();
						},
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}
}
