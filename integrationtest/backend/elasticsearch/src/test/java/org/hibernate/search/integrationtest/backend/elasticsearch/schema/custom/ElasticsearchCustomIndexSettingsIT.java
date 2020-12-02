/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchCustomIndexSettingsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Test
	public void valid() {
		setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"custom-index-settings/valid.json"
				).setup();
		initData( 12 );

		List<DocumentReference> hits = index.createScope().query()
				.where( f -> f.match().field( "string" ).matching( "value3" ) )
				.fetchHits( 12 );

		assertThat( hits ).hasSize( 3 );
	}

	@Test
	public void notExisting() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"custom-index-settings/not-existing.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to find the given custom index settings file:",
						"custom-index-settings/not-existing.json"
				);
	}

	@Test
	public void notParsable() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"custom-index-settings/not-parsable.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"There are some JSON syntax errors on the given custom index settings file:",
						"custom-index-settings/not-parsable.json"
				);
	}

	@Test
	public void unknownSetting() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"custom-index-settings/unknown-setting.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during bootstrap",
						"Elasticsearch response indicates a failure",
						"illegal_argument_exception", "unknown setting", "index.unknown_setting"
				);
	}

	private void initData(int documentCount) {
		index.bulkIndexer()
				.add( documentCount, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().string, "value" + ( i % 4 ) )
				) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
