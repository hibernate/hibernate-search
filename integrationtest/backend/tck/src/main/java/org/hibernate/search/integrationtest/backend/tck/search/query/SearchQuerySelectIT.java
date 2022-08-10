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

	private static final SimpleMappedIndex<AbstractEntityProjectionIT.IndexBinding> selectEntityIndex =
			SimpleMappedIndex.of( AbstractEntityProjectionIT.IndexBinding::new ).name( "entity" );
	private static final StubMappedIndex selectEntityReferenceIndex = StubMappedIndex.withoutFields()
			.name( "entityref" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( selectEntityIndex, selectEntityReferenceIndex ).setup();

		BulkIndexer selectEntityIndexer = selectEntityIndex.bulkIndexer();
		AbstractEntityProjectionIT.initData( selectEntityIndex, selectEntityIndexer );
		BulkIndexer selectEntityReferenceIndexer = selectEntityReferenceIndex.bulkIndexer();
		AbstractEntityReferenceProjectionIT.initData( selectEntityReferenceIndexer );
		selectEntityIndexer.join( selectEntityReferenceIndexer );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	public static class SelectDefaultIT extends AbstractEntityProjectionIT {
		public SelectDefaultIT() {
			super( selectEntityIndex );
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step;
		}
	}

	@Nested
	public static class SelectEntityIT extends AbstractEntityProjectionIT {
		public SelectEntityIT() {
			super( selectEntityIndex );
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
			super( selectEntityIndex );
		}

		@Override
		public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
			return step.extension( TckConfiguration.get().getBackendHelper().queryDslExtension() );
		}
	}

	@Nested
	public static class SelectEntityWithExtensionIT extends AbstractEntityProjectionIT {
		public SelectEntityWithExtensionIT() {
			super( selectEntityIndex );
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
