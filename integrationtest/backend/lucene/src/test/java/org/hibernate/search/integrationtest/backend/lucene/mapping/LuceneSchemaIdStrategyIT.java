/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.schema.SchemaIdStrategy;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class LuceneSchemaIdStrategyIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public void setup(SchemaIdStrategy strategy) {
		setupHelper.start()
				.withBackendProperty( LuceneBackendSettings.SCHEMA_ID_STRATEGY, strategy )
				.withIndex( index )
				.setup();

		initData();
	}

	private void test() {
		assertThatQuery( index.createScope().query()
				.select( f -> f.id() )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						"ID:1"
				);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void lucene8() {
		setup( SchemaIdStrategy.LUCENE_8 );
		test();
	}

	@Test
	public void lucene9() {
		setup( SchemaIdStrategy.LUCENE_9 );

		test();
	}

	private void initData() {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( index.binding().string, "keyword" );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "keyword", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}
}
