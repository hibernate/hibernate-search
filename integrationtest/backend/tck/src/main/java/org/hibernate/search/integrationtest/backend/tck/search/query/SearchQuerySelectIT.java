/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.search.projection.AbstractEntityProjectionIT;
import org.hibernate.search.integrationtest.backend.tck.search.projection.AbstractEntityReferenceProjectionIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class SearchQuerySelectIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

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

	@BeforeAll
	static void setup() {
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

	@Nested
	class SelectDefaultIT extends AbstractEntityProjectionIT {
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
	class SelectEntityIT extends AbstractEntityProjectionIT {
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
	class SelectEntityReferenceIT extends AbstractEntityReferenceProjectionIT {
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
	class SelectDefaultWithExtensionIT extends AbstractEntityProjectionIT {
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
	class SelectEntityWithExtensionIT extends AbstractEntityProjectionIT {
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
	class SelectEntityReferenceWithExtensionIT extends AbstractEntityReferenceProjectionIT {
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
