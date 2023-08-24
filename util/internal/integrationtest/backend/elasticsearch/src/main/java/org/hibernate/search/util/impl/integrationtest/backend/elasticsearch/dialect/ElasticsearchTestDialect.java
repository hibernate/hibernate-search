/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.util.Comparator;
import java.util.function.Predicate;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;

public class ElasticsearchTestDialect {

	private static ElasticsearchVersion actualVersion;

	private static final ElasticsearchTestDialect INSTANCE = new ElasticsearchTestDialect();
	private static final String LOCAL_DATE_DEFAULT_FORMAT = "uuuu-MM-dd";

	public static ElasticsearchTestDialect get() {
		return INSTANCE;
	}

	public static ElasticsearchVersion getActualVersion() {
		if ( actualVersion == null ) {
			ElasticsearchDistributionName distribution =
					ElasticsearchDistributionName.of( System.getProperty(
							"org.hibernate.search.integrationtest.backend.elasticsearch.distribution" ) );
			String versionString = System.getProperty(
					"org.hibernate.search.integrationtest.backend.elasticsearch.version" );
			actualVersion = ElasticsearchVersion.of( distribution, versionString.isBlank() ? null : versionString );
		}
		return actualVersion;
	}

	public String getLocalDateDefaultMappingFormat() {
		return LOCAL_DATE_DEFAULT_FORMAT;
	}

	public boolean supportsExplicitPurge() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	public static boolean isActualVersion(
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate
	) {
		return isActualVersion(
				elasticsearchPredicate,
				openSearchPredicate,
				null
		);
	}

	public static boolean isActualVersion(
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate,
			Predicate<ElasticsearchVersionCondition> amazonOpenSearchServerlessPredicate
	) {
		return isVersion(
				getActualVersion(),
				elasticsearchPredicate,
				openSearchPredicate,
				amazonOpenSearchServerlessPredicate
		);
	}

	static boolean isVersion(
			ElasticsearchVersion version,
			Predicate<ElasticsearchVersionCondition> elasticsearchPredicate,
			Predicate<ElasticsearchVersionCondition> openSearchPredicate,
			Predicate<ElasticsearchVersionCondition> amazonOpenSearchServerlessPredicate
	) {
		ElasticsearchVersionCondition condition = new ElasticsearchVersionCondition( version );

		switch ( version.distribution() ) {
			case ELASTIC:
				return elasticsearchPredicate.test( condition );
			case OPENSEARCH:
				return openSearchPredicate.test( condition );
			case AMAZON_OPENSEARCH_SERVERLESS:
				if ( amazonOpenSearchServerlessPredicate != null ) {
					return amazonOpenSearchServerlessPredicate.test( condition );
				}
				else {
					// The caller doesn't handle AOSS; let's behave as if we were on the latest OpenSearch version.
					condition = new ElasticsearchVersionCondition( ElasticsearchVersion.of( "opensearch:999.999" ) );
					return openSearchPredicate.test( condition );
				}
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

			return Comparator.comparing( (ElasticsearchVersion version) -> version.majorOptional().orElse( defaultInt ) )
					.thenComparing( version -> version.minor().orElse( defaultInt ) )
					.thenComparing( version -> version.micro().orElse( defaultInt ) )
					.compare( a, b );
		}
	}
}
