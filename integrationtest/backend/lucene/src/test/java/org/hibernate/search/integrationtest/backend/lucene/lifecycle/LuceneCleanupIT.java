/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.directory.OpenResourceTracker;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.directory.TrackingDirectoryProvider;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test that the Lucene backend correctly releases open resources.
 */
@RunWith(Parameterized.class)
public class LuceneCleanupIT {

	@Parameterized.Parameters(name = "commit_interval {0}, refresh_interval {0}")
	public static Object[][] strategies() {
		return new Object[][] {
				{ null, null },
				{ 0, 0 },
				{ 0, 1_000 },
				{ 1_000, 0 },
				{ 1_000, 1_000 }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final Integer commitInterval;
	private final Integer refreshInterval;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public LuceneCleanupIT(Integer commitInterval, Integer refreshInterval) {
		this.commitInterval = commitInterval;
		this.refreshInterval = refreshInterval;
	}

	@Test
	public void test() {
		OpenResourceTracker tracker = new OpenResourceTracker();
		StubMapping mapping = setup( tracker );

		// Execute a few operations to be sure that we open files
		doQuery();
		doStore( 1, 2, 3 );
		doQuery();
		doStore( 4, 5, 6, 7, 8 );
		doQuery();

		assertThat( tracker.openResources() ).isNotEmpty();

		mapping.close();

		assertThat( tracker.openResources() ).isEmpty();
	}

	private void doQuery() {
		IndexBinding binding = index.binding();
		index.query()
				.select( f -> f.composite(
						f.field( binding.text.relativeFieldName ),
						f.field( binding.keyword.relativeFieldName ),
						f.field( binding.integer.relativeFieldName ) ) )
				.where( f -> f.range().field( binding.integer.relativeFieldName ).between( 0, 10_000 ) )
				.aggregation( AggregationKey.of( "keyword" ),
						f -> f.terms().field( binding.keyword.relativeFieldName, String.class ) )
				.aggregation( AggregationKey.of( "integer" ),
						f -> f.range().field( binding.integer.relativeFieldName, Integer.class )
								.range( 0, 2 )
								.range( 2, null ) )
				.sort( f -> f.field( binding.keyword.relativeFieldName ).then()
						.field( binding.integer.relativeFieldName ) )
				.fetchAll();
	}

	private void doStore(int ... ids) {
		IndexIndexingPlan plan = index.createIndexingPlan(
				// Let the commit/refresh intervals do their job
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		);
		IndexBinding binding = index.binding();
		for ( int id : ids ) {
			String idString = String.valueOf( id );
			plan.add( referenceProvider( idString ), document -> {
				document.addValue( binding.text.reference,
						"This is some text that contains " + idString + " (which is the ID)." );
				document.addValue( binding.keyword.reference, idString );
				document.addValue( binding.integer.reference, id );
			} );
		}
		plan.execute( OperationSubmitter.BLOCKING ).join();
	}

	private StubMapping setup(OpenResourceTracker tracker) {
		return setupHelper.start()
				.withIndex( index )
				.withBackendProperty( LuceneIndexSettings.IO_COMMIT_INTERVAL, commitInterval )
				.withBackendProperty( LuceneIndexSettings.IO_REFRESH_INTERVAL, refreshInterval )
				.withBackendProperty( LuceneIndexSettings.DIRECTORY_TYPE,
						(BeanReference<TrackingDirectoryProvider>) beanResolver -> {
							BeanHolder<DirectoryProvider> delegateHolder = beanResolver.resolve( DirectoryProvider.class,
									"local-filesystem", BeanRetrieval.ANY
							);
							return BeanHolder.of( new TrackingDirectoryProvider( delegateHolder.get(), tracker ) )
									.withDependencyAutoClosing( delegateHolder );
						} )
				.setup();
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> text;
		final SimpleFieldModel<String> keyword;
		final SimpleFieldModel<Integer> integer;

		IndexBinding(IndexSchemaElement root) {
			// Try to use multiple features that might involve different files (docvalues, storage, ...)
			text = SimpleFieldModel.mapper( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					o -> o.projectable( Projectable.YES ) )
					.map( root, "text" );
			keyword = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE,
					o -> o.projectable( Projectable.YES ).sortable( Sortable.YES ).aggregable( Aggregable.YES ) )
					.map( root, "keyword" );
			integer = SimpleFieldModel.mapper( IntegerFieldTypeDescriptor.INSTANCE,
					o -> o.projectable( Projectable.YES ).sortable( Sortable.YES ).aggregable( Aggregable.YES ) )
					.map( root, "integer" );
		}
	}
}
