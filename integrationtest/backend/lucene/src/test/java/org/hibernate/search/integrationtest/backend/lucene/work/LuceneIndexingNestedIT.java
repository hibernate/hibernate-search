/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.util.Collections;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test indexing, and more importantly updates and deletions,
 * when nested documents are involved.
 */
@TestForIssue(jiraKey = "HSEARCH-3834")
public class LuceneIndexingNestedIT {

	private static final String INDEX_NAME = "indexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubBackendSessionContext sessionContext;

	@Test
	public void add() throws IOException {
		setup( MultiTenancyStrategyName.NONE );

		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 1 );
	}

	@Test
	public void update_byTerm() throws IOException {
		// No multitenancy, which means the backend will use indexWriter.updateDocuments(Term, Iterable) for updates
		setup( MultiTenancyStrategyName.NONE );

		IndexIndexingPlan plan = indexManager.createIndexingPlan( sessionContext );
		plan.update( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( indexMapping.nestedObject.self );
			nested.addValue( indexMapping.nestedObject.field2, "value" );
		} );
		plan.execute().join();

		assertThat( countWithField( "nestedObject.field2" ) ).isEqualTo( 1 );
		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void update_byQuery() throws IOException {
		// Multitenancy enabled, which means the backend will use
		// indexWriter.deleteDocuments(Query) then indexWriter.addDocument for updates
		setup( MultiTenancyStrategyName.DISCRIMINATOR );

		IndexIndexingPlan plan = indexManager.createIndexingPlan( sessionContext );
		plan.update( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( indexMapping.nestedObject.self );
			nested.addValue( indexMapping.nestedObject.field2, "value" );
		} );
		plan.execute().join();

		assertThat( countWithField( "nestedObject.field2" ) ).isEqualTo( 1 );
		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void delete_byTerm() throws IOException {
		// No multitenancy, which means the backend will use indexWriter.deleteDocuments(Term) for deletion
		setup( MultiTenancyStrategyName.NONE );

		IndexIndexingPlan plan = indexManager.createIndexingPlan( sessionContext );
		plan.delete( referenceProvider( "1" ) );
		plan.execute().join();

		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void delete_byQuery() throws IOException {
		// Multitenancy enabled, which means the backend will use indexWriter.deleteDocuments(Query) for deletion
		setup( MultiTenancyStrategyName.DISCRIMINATOR );

		IndexIndexingPlan plan = indexManager.createIndexingPlan( sessionContext );
		plan.delete( referenceProvider( "1" ) );
		plan.execute().join();

		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void purge() throws IOException {
		setup( MultiTenancyStrategyName.NONE );

		indexManager.createWorkspace( sessionContext ).purge( Collections.emptySet() ).join();
		indexManager.createWorkspace( sessionContext ).refresh().join();

		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	private void setup(MultiTenancyStrategyName multiTenancyStrategyName) throws IOException {
		setupHelper.start()
				.withBackendProperty( LuceneBackendSettings.MULTI_TENANCY_STRATEGY, multiTenancyStrategyName )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		assertThat( countWithField( "field1" ) ).isEqualTo( 0 );

		if ( MultiTenancyStrategyName.NONE.equals( multiTenancyStrategyName ) ) {
			sessionContext = new StubBackendSessionContext();
		}
		else {
			sessionContext = new StubBackendSessionContext( "someTenantId" );
		}

		IndexIndexingPlan plan = indexManager.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( indexMapping.nestedObject.self );
			nested.addValue( indexMapping.nestedObject.field1, "value" );
		} );
		plan.execute().join();
	}

	private int countWithField(String absoluteFieldPath) throws IOException {
		return LuceneIndexContentUtils.doOnIndexCopy(
				setupHelper, temporaryFolder, INDEX_NAME,
				reader -> reader.getDocCount( absoluteFieldPath )
		);
	}

	private static class IndexMapping {
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED )
							.multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> field1;
		final IndexFieldReference<String> field2;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			field1 = objectField.field( "field1", f -> f.asString() ).toReference();
			field2 = objectField.field( "field2", f -> f.asString() ).toReference();
		}
	}
}
