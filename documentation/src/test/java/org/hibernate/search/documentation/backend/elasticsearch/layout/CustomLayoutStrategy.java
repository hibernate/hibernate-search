/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.layout;

// tag::include[]
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;

public class CustomLayoutStrategy implements IndexLayoutStrategy {

	private static final DateTimeFormatter INDEX_SUFFIX_FORMATTER =
			DateTimeFormatter.ofPattern( "uuuuMMdd-HHmmss-SSSSSSSSS", Locale.ROOT )
					.withZone( ZoneOffset.UTC );
	private static final Pattern UNIQUE_KEY_PATTERN =
			Pattern.compile( "(.*)-\\d+-\\d+-\\d+" );

	@Override
	public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
		// Clock is Clock.systemUTC() in production, may be overridden in tests
		Clock clock = MyApplicationClock.get();
		return hibernateSearchIndexName + "-"
				+ INDEX_SUFFIX_FORMATTER.format( Instant.now( clock ) );
	}

	@Override
	public String createWriteAlias(String hibernateSearchIndexName) {
		return hibernateSearchIndexName + "-write";
	}

	@Override
	public String createReadAlias(String hibernateSearchIndexName) {
		return hibernateSearchIndexName;
	}

	@Override
	public String extractUniqueKeyFromHibernateSearchIndexName(
			String hibernateSearchIndexName) {
		return hibernateSearchIndexName;
	}

	@Override
	public String extractUniqueKeyFromElasticsearchIndexName(
			String elasticsearchIndexName) {
		Matcher matcher = UNIQUE_KEY_PATTERN.matcher( elasticsearchIndexName );
		if ( !matcher.matches() ) {
			throw new IllegalArgumentException(
					"Unrecognized index name: " + elasticsearchIndexName
			);
		}
		return matcher.group( 1 );
	}
}
// end::include[]
