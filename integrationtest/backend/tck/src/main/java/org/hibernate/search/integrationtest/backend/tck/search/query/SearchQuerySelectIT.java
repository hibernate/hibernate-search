/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.search.projection.AbstractEntityProjectionIT;
import org.hibernate.search.integrationtest.backend.tck.search.projection.AbstractEntityReferenceProjectionIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NestedRunner.class)
public class SearchQuerySelectIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityMainIndex =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entityMain" );
	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityMultiIndex1 =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entity1" );
	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityMultiIndex2 =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entity2" );
	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityMultiIndex3 =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entity3" );
	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityMultiIndex4 =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entity4" );
	private static final StubMappedIndex selectEntityReferenceIndex = StubMappedIndex.withoutFields()
			.name( "entityref" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( selectEntityMainIndex, selectEntityMultiIndex1, selectEntityMultiIndex2,
						selectEntityMultiIndex3, selectEntityMultiIndex4 )
				.withIndex( selectEntityReferenceIndex )
				.setup();

		final BulkIndexer selectEntityMainIndexer = selectEntityMainIndex.bulkIndexer();
		final BulkIndexer selectEntityMultiIndex1Indexer = selectEntityMultiIndex1.bulkIndexer();
		final BulkIndexer selectEntityMultiIndex2Indexer = selectEntityMultiIndex2.bulkIndexer();
		final BulkIndexer selectEntityMultiIndex3Indexer = selectEntityMultiIndex3.bulkIndexer();
		final BulkIndexer selectEntityMultiIndex4Indexer = selectEntityMultiIndex4.bulkIndexer();
		AbstractEntityProjectionIT.initData( selectEntityMainIndex, selectEntityMainIndexer,
				selectEntityMultiIndex1, selectEntityMultiIndex1Indexer,
				selectEntityMultiIndex2, selectEntityMultiIndex2Indexer,
				selectEntityMultiIndex3, selectEntityMultiIndex3Indexer,
				selectEntityMultiIndex4, selectEntityMultiIndex4Indexer );
		final BulkIndexer selectEntityReferenceIndexer = selectEntityReferenceIndex.bulkIndexer();
		AbstractEntityReferenceProjectionIT.initData( selectEntityReferenceIndexer );

		selectEntityMainIndexer.join( selectEntityMultiIndex1Indexer, selectEntityMultiIndex2Indexer,
				selectEntityMultiIndex3Indexer, selectEntityMultiIndex4Indexer,
				selectEntityReferenceIndexer );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	public static class SelectDefaultIT extends AbstractEntityProjectionIT {
		public SelectDefaultIT() {
			super( selectEntityMainIndex,
					selectEntityMultiIndex1, selectEntityMultiIndex2,
					selectEntityMultiIndex3, selectEntityMultiIndex4 );
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step;
		}
	}

	@Nested
	public static class SelectEntityIT extends AbstractEntityProjectionIT {
		public SelectEntityIT() {
			super( selectEntityMainIndex, selectEntityMultiIndex1, selectEntityMultiIndex2, selectEntityMultiIndex3,
					selectEntityMultiIndex4
			);
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.selectEntity();
		}
	}

	@Nested
	public static class SelectEntityReferenceIT extends AbstractEntityReferenceProjectionIT {
		public SelectEntityReferenceIT() {
			super( selectEntityReferenceIndex );
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, R, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.selectEntityReference();
		}
	}

	// The backend extensions have to redefine the implementation of .select methods in
	// order to use their specialized return type,
	// so we need to test them separately.

	@Nested
	public static class SelectDefaultWithExtensionIT extends AbstractEntityProjectionIT {
		public SelectDefaultWithExtensionIT() {
			super( selectEntityMainIndex, selectEntityMultiIndex1, selectEntityMultiIndex2, selectEntityMultiIndex3,
					selectEntityMultiIndex4
			);
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.extension( TckConfiguration.get().getBackendHelper().queryDslExtension() );
		}
	}

	@Nested
	public static class SelectEntityWithExtensionIT extends AbstractEntityProjectionIT {
		public SelectEntityWithExtensionIT() {
			super( selectEntityMainIndex, selectEntityMultiIndex1, selectEntityMultiIndex2, selectEntityMultiIndex3,
					selectEntityMultiIndex4
			);
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.extension( TckConfiguration.get().getBackendHelper().<R, E, LOS>queryDslExtension() )
					.selectEntity();
		}
	}

	@Nested
	public static class SelectEntityReferenceWithExtensionIT extends AbstractEntityReferenceProjectionIT {
		public SelectEntityReferenceWithExtensionIT() {
			super( selectEntityReferenceIndex );
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, R, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.extension( TckConfiguration.get().getBackendHelper().<R, E, LOS>queryDslExtension() )
					.selectEntityReference();
		}
	}
}
