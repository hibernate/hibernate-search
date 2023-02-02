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
import java.util.EnumSet;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LuceneIndexSchemaManagerCreationOrPreservationIT {

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<LuceneIndexSchemaManagerOperation> operations() {
		return LuceneIndexSchemaManagerOperation.creatingOrPreserving();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final LuceneIndexSchemaManagerOperation operation;

	public LuceneIndexSchemaManagerCreationOrPreservationIT(LuceneIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void doesNotExist() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		createOrPreserve();

		assertThat( indexExists() ).isTrue();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void alreadyExists() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		createOrPreserve();

		assertThat( indexExists() ).isTrue();

		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {
		} );
		plan.execute( OperationSubmitter.blocking() ).join();

		assertThat( countDocsOnDisk() ).isEqualTo( 1 );

		createOrPreserve();

		assertThat( indexExists() ).isTrue();
		assertThat( countDocsOnDisk() ).isEqualTo( 1 );
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, index.name() );
	}

	private int countDocsOnDisk() throws IOException {
		return LuceneIndexContentUtils.readIndex(
				setupHelper, index.name(),
				reader -> reader.getDocCount( MetadataFields.idFieldName() )
		);
	}

	private void createOrPreserve() {
		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager(), OperationSubmitter.blocking() ) );
	}

	private void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
	}
}
