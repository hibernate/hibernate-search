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
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test indexing, and more importantly updates and deletions,
 * when nested documents are involved.
 */
@TestForIssue(jiraKey = "HSEARCH-3834")
public class LuceneIndexingNestedIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private StubSession sessionContext;

	@Test
	public void add() throws IOException {
		setup( MultiTenancyStrategyName.NONE );

		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 1 );
	}

	@Test
	public void update_byTerm() throws IOException {
		// No multitenancy, which means the backend will use indexWriter.updateDocuments(Term, Iterable) for updates
		setup( MultiTenancyStrategyName.NONE );

		IndexIndexingPlan plan = index.createIndexingPlan( sessionContext );
		plan.addOrUpdate( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( index.binding().nestedObject.self );
			nested.addValue( index.binding().nestedObject.field2, "value" );
		} );
		plan.execute( OperationSubmitter.blocking() ).join();

		assertThat( countWithField( "nestedObject.field2" ) ).isEqualTo( 1 );
		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void update_byQuery() throws IOException {
		// Multitenancy enabled, which means the backend will use
		// indexWriter.deleteDocuments(Query) then indexWriter.addDocument for updates
		setup( MultiTenancyStrategyName.DISCRIMINATOR );

		IndexIndexingPlan plan = index.createIndexingPlan( sessionContext );
		plan.addOrUpdate( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( index.binding().nestedObject.self );
			nested.addValue( index.binding().nestedObject.field2, "value" );
		} );
		plan.execute( OperationSubmitter.blocking() ).join();

		assertThat( countWithField( "nestedObject.field2" ) ).isEqualTo( 1 );
		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void delete_byTerm() throws IOException {
		// No multitenancy, which means the backend will use indexWriter.deleteDocuments(Term) for deletion
		setup( MultiTenancyStrategyName.NONE );

		IndexIndexingPlan plan = index.createIndexingPlan( sessionContext );
		plan.delete( referenceProvider( "1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void delete_byQuery() throws IOException {
		// Multitenancy enabled, which means the backend will use indexWriter.deleteDocuments(Query) for deletion
		setup( MultiTenancyStrategyName.DISCRIMINATOR );

		IndexIndexingPlan plan = index.createIndexingPlan( sessionContext );
		plan.delete( referenceProvider( "1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// This used to fail before HSEARCH-3834, because the nested document was left in the index.
		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	@Test
	public void purge() throws IOException {
		setup( MultiTenancyStrategyName.NONE );

		index.createWorkspace( sessionContext ).purge( Collections.emptySet(), OperationSubmitter.blocking() ).join();
		index.createWorkspace( sessionContext ).refresh( OperationSubmitter.blocking() ).join();

		assertThat( countWithField( "nestedObject.field1" ) ).isEqualTo( 0 );
	}

	private void setup(MultiTenancyStrategyName multiTenancyStrategyName) throws IOException {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start()
				.withBackendProperty( LuceneBackendSettings.MULTI_TENANCY_STRATEGY, multiTenancyStrategyName )
				.withIndex( index );

		if ( MultiTenancyStrategyName.DISCRIMINATOR.equals( multiTenancyStrategyName ) ) {
			// make the mapping consistent with the backend
			setupContext.withMultiTenancy();
		}

		StubMapping mapping = setupContext.setup();

		assertThat( countWithField( "field1" ) ).isEqualTo( 0 );

		if ( MultiTenancyStrategyName.NONE.equals( multiTenancyStrategyName ) ) {
			sessionContext = mapping.session();
		}
		else {
			sessionContext = mapping.session( "someTenantId" );
		}

		IndexIndexingPlan plan = index.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> {
			DocumentElement nested = document.addObject( index.binding().nestedObject.self );
			nested.addValue( index.binding().nestedObject.field1, "value" );
		} );
		plan.execute( OperationSubmitter.blocking() ).join();
	}

	private int countWithField(String absoluteFieldPath) throws IOException {
		return LuceneIndexContentUtils.readIndex(
				setupHelper, index.name(),
				reader -> reader.getDocCount( absoluteFieldPath )
		);
	}

	private static class IndexBinding {
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectStructure.NESTED )
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
