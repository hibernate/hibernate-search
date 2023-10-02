/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.util.common.impl.CollectionHelper;

final class HibernateSearch5Properties {

	private HibernateSearch5Properties() {
	}

	private static final Set<String> OBSOLETE_ROOT_PROPERTY_KEYS = CollectionHelper.asImmutableSet(
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

	private static final Set<String> OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIXES = CollectionHelper.asImmutableSet(
			"directory_provider",
			"sharding_strategy",
			"sharding_strategy.nbr_of_shards",
			"worker.backend",
			"worker.execution",
			"reader.strategy",
			"reader.async_refresh_period_ms",
			"reader.AAA",
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

	private static final Set<Pattern> OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIX_PATTERNS;
	static {
		Set<String> patternStrings = CollectionHelper.asImmutableSet(
				"reader\\..*",
				"indexwriter\\..*"
		);
		Set<Pattern> result = new LinkedHashSet<>();
		for ( String patternString : patternStrings ) {
			result.add( Pattern.compile( patternString ) );
		}
		OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIX_PATTERNS = Collections.unmodifiableSet( result );
	}

	static boolean isSearch5PropertyKey(String propertyKey) {
		String hibernateSearchPrefix = "hibernate.search.";
		if ( !propertyKey.startsWith( hibernateSearchPrefix ) ) {
			// Not a Hibernate Search 5 property
			return false;
		}

		// Check for obsolete root-level properties.
		if ( OBSOLETE_ROOT_PROPERTY_KEYS.contains( propertyKey ) ) {
			return true;
		}

		// Check for obsolete index-level properties.
		int dotAfterIndexNameIndex = propertyKey.indexOf( '.', hibernateSearchPrefix.length() );
		if ( dotAfterIndexNameIndex < 0 ) {
			// Not an index-level or shard-level property: there is no prefix that could be interpreted as an index name.
			return false;
		}
		if ( isSearch5IndexOrShardLevelPropertySuffix( propertyKey.substring( dotAfterIndexNameIndex + 1 ) ) ) {
			return true;
		}

		// Check for obsolete shard-level properties
		int dotAfterShardNameIndex = propertyKey.indexOf( '.', dotAfterIndexNameIndex + 1 );
		if ( dotAfterShardNameIndex < 0 ) {
			// Not a shard-level property: there is no prefix that could be interpreted as a shard index or shard name.
			return false;
		}
		if ( isSearch5IndexOrShardLevelPropertySuffix( propertyKey.substring( dotAfterShardNameIndex + 1 ) ) ) {
			return true;
		}

		return false;
	}

	private static boolean isSearch5IndexOrShardLevelPropertySuffix(String suffix) {
		if ( OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIXES.contains( suffix ) ) {
			return true;
		}
		for ( Pattern pattern : OBSOLETE_INDEX_LEVEL_PROPERTY_KEY_SUFFIX_PATTERNS ) {
			if ( pattern.matcher( suffix ).matches() ) {
				return true;
			}
		}
		return false;
	}

}
