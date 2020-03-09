/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LuceneIndexSchemaManagerCreationIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<LuceneIndexSchemaManagerOperation> operations() {
		return LuceneIndexSchemaManagerOperation.creating();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final LuceneIndexSchemaManagerOperation operation;

	private StubMappingIndexManager indexManager;

	public LuceneIndexSchemaManagerCreationIT(LuceneIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void simple() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		create();

		assertThat( indexExists() ).isTrue();
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, INDEX_NAME );
	}

	private void create() {
		Futures.unwrappedExceptionJoin( operation.apply( indexManager.getSchemaManager() ) );
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
