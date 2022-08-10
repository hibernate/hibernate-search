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

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new );

	public EntityProjectionIT() {
		super( mainIndex );
	}

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( mainIndex ).setup();

		BulkIndexer indexer = mainIndex.bulkIndexer();
		initData( mainIndex, indexer );
		indexer.join();
	}

	@Override
	public <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(SearchQuerySelectStep<?, R, E, LOS, ?, ?> step) {
		return step.select( f -> f.entity() );
	}
}
