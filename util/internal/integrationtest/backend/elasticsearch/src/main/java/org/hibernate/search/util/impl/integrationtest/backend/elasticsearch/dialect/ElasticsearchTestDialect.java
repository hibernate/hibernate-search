/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;

public class ElasticsearchTestDialect {

	private static final ElasticsearchVersion ACTUAL_VERSION = ElasticsearchVersion.of(
			System.getProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" )
	);

	private static final ElasticsearchTestDialect INSTANCE = new ElasticsearchTestDialect();

	public static ElasticsearchTestDialect get() {
		return INSTANCE;
	}

	public static ElasticsearchVersion getActualVersion() {
		return ACTUAL_VERSION;
	}

	public boolean isEmptyMappingPossible() {
		return isActualVersion(
				esVersion -> esVersion.isAtMost( "6.8" ),
				osVersion -> false
		);
	}

	@SuppressWarnings("deprecation")
	public Optional<URLEncodedString> getTypeNameForMappingAndBulkApi() {
		if ( isActualVersion(
				esVersion -> esVersion.isAtMost( "6.8" ),
				osVersion -> false
		) ) {
			return Optional.of( Paths.DOC );
		}
		return Optional.empty();
	}

	public Boolean getIncludeTypeNameParameterForMappingApi() {
		if ( isActualVersion(
				esVersion -> esVersion.isBetween( "6.7", "6.8" ),
				osVersion -> false
		) ) {
			return Boolean.TRUE;
		}
		return null;
	}

	public List<String> getAllLocalDateDefaultMappingFormats() {
		if ( isActualVersion(
				esVersion -> esVersion.isAtMost( "6.8" ),
				osVersion -> false
		) ) {
			return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
		}
		return Collections.singletonList( "uuuu-MM-dd" );
	}

	public boolean supportsIsWriteIndex() {
		return isActualVersion(
				esVersion -> !esVersion.isLessThan( "6.4.0" ),
				osVersion -> true
		);
	}

	public String getFirstLocalDateDefaultMappingFormat() {
		return getAllLocalDateDefaultMappingFormats().get( 0 );
	}

	public String getConcatenatedLocalDateDefaultMappingFormats() {
		return String.join( "||", getAllLocalDateDefaultMappingFormats() );
	}

	public static boolean isActualVersion(
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> opensearchPredicate
	) {
		return isVersion(
				ACTUAL_VERSION,
				elasticsearchPredicate,
				opensearchPredicate
		);
	}

	static boolean isVersion(
			ElasticsearchVersion version,
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> opensearchPredicate
	) {
		ElasticsearchVersionCondition condition = new ElasticsearchVersionCondition( version );

		switch ( version.distribution() ) {
			case ELASTIC:
				return elasticsearchPredicate.test( condition );
			case OPENSEARCH:
				return opensearchPredicate.test( condition );
			default:
				throw new IllegalStateException( "Unknown distribution" );
		}
	}

	public static class ElasticsearchVersionCondition {
		private final ElasticsearchVersion actual;

		private ElasticsearchVersionCondition(ElasticsearchVersion actual) {
			this.actual = actual;
		}

		public boolean isAws() {
			return ElasticsearchTestHostConnectionConfiguration.get().isAws();
		}

		public boolean isMatching(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return v.matches( actual );
		}

		public boolean isAtMost(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return compare( actual, v, Integer.MAX_VALUE ) <= 0;
		}

		public boolean isLessThan(String version) {
			ElasticsearchVersion v = ElasticsearchVersion.of( actual.distribution(), version );

			return compare( actual, v, Integer.MAX_VALUE ) < 0;
		}

		public boolean isBetween(String minVersion, String maxVersion) {
			ElasticsearchVersion min = ElasticsearchVersion.of( actual.distribution(), minVersion );
			ElasticsearchVersion max = ElasticsearchVersion.of( actual.distribution(), maxVersion );

			return !( compare( max, actual, Integer.MAX_VALUE ) < 0 || compare( min, actual, Integer.MIN_VALUE ) > 0 );
		}

		public ElasticsearchVersion actual() {
			return actual;
		}

		private static int compare(ElasticsearchVersion a, ElasticsearchVersion b, int defaultInt) {
			if ( !a.distribution().equals( b.distribution() ) ) {
				throw new IllegalArgumentException( "Cannot compare different distributions" );
			}

			if ( a.qualifier().isPresent() || b.qualifier().isPresent() ) {
				throw new IllegalArgumentException( "Qualifiers are ignored for version ranges." );
			}

			return Comparator.comparing( ElasticsearchVersion::major )
					.thenComparing( version -> version.minor().orElse( defaultInt ) )
					.thenComparing( version -> version.micro().orElse( defaultInt ) )
					.compare( a, b );
		}
	}
}
