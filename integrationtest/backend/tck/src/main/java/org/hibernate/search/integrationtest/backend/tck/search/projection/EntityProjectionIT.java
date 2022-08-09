/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;

public class EntityProjectionIT extends AbstractEntityProjectionIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "main" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex1 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi1" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex2 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi2" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex3 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi3" );
	private static final SimpleMappedIndex<IndexBinding> multiIndex4 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "multi4" );

	public EntityProjectionIT() {
		super( mainIndex, multiIndex1, multiIndex2, multiIndex3, multiIndex4 );
	}

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, multiIndex1, multiIndex2, multiIndex3, multiIndex4 ).setup();

		final BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		final BulkIndexer multiIndex1Indexer = multiIndex1.bulkIndexer();
		final BulkIndexer multiIndex2Indexer = multiIndex2.bulkIndexer();
		final BulkIndexer multiIndex3Indexer = multiIndex3.bulkIndexer();
		final BulkIndexer multiIndex4Indexer = multiIndex4.bulkIndexer();
		initData( mainIndex, mainIndexer,
				multiIndex1, multiIndex1Indexer, multiIndex2, multiIndex2Indexer,
				multiIndex3, multiIndex3Indexer, multiIndex4, multiIndex4Indexer );

		mainIndexer.join( multiIndex1Indexer, multiIndex2Indexer, multiIndex3Indexer, multiIndex4Indexer );
	}

	@Override
	public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
		return step.select( f -> f.entity() );
	}
}
