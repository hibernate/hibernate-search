/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.naming.impl;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The default naming strategy for indexes:
 * <ul>
 *     <li>Elasticsearch index names follow the format used by Elasticsearch's Rollover API: {@code <hsearchname>-<6 digits>}.
 *     See {@code <target-index>} here:
 *     https://www.elastic.co/guide/en/elasticsearch/reference/master/indices-rollover-index.html#rollover-index-api-path-params
 *     </li>
 *     <li>The write alias is {@code <hsearchname>-write}.
 *     <li>The read alias is {@code <hsearchname>-read}.
 * </ul>
 */
public final class DefaultIndexNamingStrategy implements IndexNamingStrategy {

	static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final Pattern UNIQUE_KEY_EXTRACTION_PATTERN = Pattern.compile( "(.*)-\\d{6}" );

	@Override
	public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
		return hibernateSearchIndexName + "-000001";
	}

	@Override
	public String createWriteAlias(String hibernateSearchIndexName) {
		return hibernateSearchIndexName + "-write";
	}

	@Override
	public String createReadAlias(String hibernateSearchIndexName) {
		return hibernateSearchIndexName + "-read";
	}

	@Override
	public String extractUniqueKeyFromHibernateSearchIndexName(String hibernateSearchIndexName) {
		return hibernateSearchIndexName;
	}

	@Override
	public String extractUniqueKeyFromElasticsearchIndexName(String elasticsearchIndexName) {
		Matcher matcher = UNIQUE_KEY_EXTRACTION_PATTERN.matcher( elasticsearchIndexName );
		if ( !matcher.matches() ) {
			throw log.invalidIndexPrimaryName( elasticsearchIndexName, UNIQUE_KEY_EXTRACTION_PATTERN );
		}
		return matcher.group( 1 );
	}
}
