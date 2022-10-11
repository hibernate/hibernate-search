/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import java.util.Comparator;
import java.util.function.BooleanSupplier;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

public final class ElasticsearchVersionUtils {
	private ElasticsearchVersionUtils() {
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

	public static boolean isOpenSearch(ElasticsearchVersion actual) {

		return isDistribution( actual, ElasticsearchDistributionName.OPENSEARCH );
	}

	private static boolean isDistribution(
			ElasticsearchVersion actual,
			ElasticsearchDistributionName distribution
	) {

		return actual.distribution().equals( distribution );
	}

	public static boolean isMatching(ElasticsearchVersion actual, String version) {
		ElasticsearchVersion v = ElasticsearchVersion.of( version );

		return v.matches( actual );
	}

	public static boolean isAtMost(ElasticsearchVersion actual, String version) {
		ElasticsearchVersion v = ElasticsearchVersion.of( version );

		return tryOrFalse(
				() -> compare( actual, v, Integer.MAX_VALUE ) <= 0
		);
	}

	public static boolean isLessThan(ElasticsearchVersion actual, String version) {
		ElasticsearchVersion v = ElasticsearchVersion.of( version );

		return tryOrFalse(
				() -> compare( actual, v, Integer.MAX_VALUE ) < 0
		);
	}

	public static boolean isBetween(ElasticsearchVersion actual, String minVersion, String maxVersion) {
		ElasticsearchVersion min = ElasticsearchVersion.of( minVersion );
		ElasticsearchVersion max = ElasticsearchVersion.of( maxVersion );

		return tryOrFalse(
				() -> !( compare( max, actual, Integer.MAX_VALUE ) < 0 || compare( min, actual, Integer.MIN_VALUE ) > 0 )
		);
	}

	private static boolean tryOrFalse(BooleanSupplier test) {
		try {
			return test.getAsBoolean();
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

}
