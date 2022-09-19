/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

import org.junit.Rule;
import org.junit.Test;

public class ObsoletePropertiesIT {

	private static final List<String> OBSOLETE_ROOT_LEVEL_PROPERTY_KEYS = CollectionHelper.asImmutableList(
			"hibernate.search.query.object_lookup_method",
			"hibernate.search.query.database_retrieval_method",
			"hibernate.search.autoregister_listeners",
			"hibernate.search.indexing_strategy",
			"hibernate.search.analyzer",
			"hibernate.search.lucene.analysis_definition_provider",
			"hibernate.search.similarity",
			"hibernate.search.worker.scope",
			"hibernate.search.worker.enlist_in_transaction",
			"hibernate.search.batch_size",
			"hibernate.search.filter.cache_strategy",
			"hibernate.search.filter.cache_docidresults.size",
			"hibernate.search.model_mapping",
			"hibernate.search.error_handler",
			"hibernate.search.jmx_enabled",
			"hibernate.search.jmx_bean_suffix",
			"hibernate.search.generate_statistics",
			"hibernate.search.lucene_version",
			"hibernate.search.default_null_token",
			"hibernate.search.enable_dirty_check",
			"hibernate.search.index_uninverting_allowed",
			"hibernate.search.default.elasticsearch.host",
			"hibernate.search.default.elasticsearch.path_prefix",
			"hibernate.search.default.elasticsearch.username",
			"hibernate.search.default.elasticsearch.password",
			"hibernate.search.default.elasticsearch.request_timeout",
			"hibernate.search.default.elasticsearch.read_timeout",
			"hibernate.search.default.elasticsearch.connection_timeout",
			"hibernate.search.default.elasticsearch.max_total_connection",
			"hibernate.search.default.elasticsearch.max_total_connection_per_route",
			"hibernate.search.default.elasticsearch.discovery.enabled",
			"hibernate.search.default.elasticsearch.discovery.refresh_interval",
			"hibernate.search.default.elasticsearch.discovery.default_scheme",
			"hibernate.search.elasticsearch.scroll_backtracking_window_size",
			"hibernate.search.elasticsearch.scroll_fetch_size",
			"hibernate.search.elasticsearch.scroll_timeout",
			"hibernate.search.elasticsearch.analysis_definition_provider",
			"hibernate.search.elasticsearch.log.json_pretty_printing"
	);

	private static final List<String> OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIXES = CollectionHelper.asImmutableList(
			"directory_provider",
			"sharding_strategy",
			"sharding_strategy.nbr_of_shards",
			"worker.backend",
			"worker.execution",
			"reader.strategy",
			"reader.async_refresh_period_ms",
			"reader.someReaderProperty",
			"reader.someOtherReaderProperty",
			"reader.someOtherReaderProperty.foo",
			"indexwriter.someWriterProperty",
			"indexwriter.someOtherWriterProperty.foo",
			"exclusive_index_use",
			"index_metadata_complete",
			"retry_marker_lookup",
			"similarity",
			"max_queue_length",
			"index_flush_interval",
			"indexmanager",
			"locking_strategy",
			"indexBase",
			"indexName",
			"elasticsearch.index_schema_management_strategy",
			"elasticsearch.index_management_wait_timeout",
			"elasticsearch.required_index_status",
			"elasticsearch.refresh_after_write",
			"elasticsearch.dynamic_mapping"
	);

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void obsoleteProperty() {
		// Generate a list of all invalid property keys we can think of
		List<String> obsoletePropertyKeys = new ArrayList<>();
		obsoletePropertyKeys.addAll( OBSOLETE_ROOT_LEVEL_PROPERTY_KEYS );
		for ( String suffix : OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIXES ) {
			obsoletePropertyKeys.add( "hibernate.search.default." + suffix );
			obsoletePropertyKeys.add( "hibernate.search.default.0." + suffix );
			obsoletePropertyKeys.add( "hibernate.search.default.myShard." + suffix );
			obsoletePropertyKeys.add( "hibernate.search.myIndex." + suffix );
			obsoletePropertyKeys.add( "hibernate.search.myIndex.0." + suffix );
			obsoletePropertyKeys.add( "hibernate.search.myIndex.myShard." + suffix );
		}

		// Start Hibernate Search with these properties and expect a failure
		assertThatThrownBy( () -> setup( builder -> {
			// Add a non-obsolete property that shouldn't be reported.
			builder.setProperty( "hibernate.search.backend.myBackend.foo", "bar" );
			// Add obsolete properties that should be reported.
			for ( String propertyKey : obsoletePropertyKeys ) {
				builder.setProperty( propertyKey, "foo" );
			}
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid configuration passed to Hibernate Search",
						"some properties in the given configuration are obsolete",
						"Configuration properties changed between Hibernate Search 5 and Hibernate Search 6",
						"Check out the reference documentation and upgrade your configuration",
						"Obsolete properties: "
				)
				// Check that all obsolete properties, and only obsolete properties, are reported.
				.extracting( e -> {
					Matcher matcher = Pattern.compile( "Obsolete properties: \\[([^]]+)]" ).matcher( e.getMessage() );
					matcher.find();
					return Arrays.asList( matcher.group( 1 ).split( ", " ) );
				} )
				.asList()
				.containsExactlyInAnyOrderElementsOf( obsoletePropertyKeys );
	}

	private void setup(Consumer<SimpleSessionFactoryBuilder> configurationContributor) {
		ormSetupHelper.start()
				.withConfiguration( configurationContributor )
				.setup( IndexedEntity.class );
	}

	@Entity
	@Table(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField(name = "myTextField")
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
