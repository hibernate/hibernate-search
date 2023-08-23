/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ElasticsearchDialectsTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> params() {
		return Arrays.asList(
				// Unsupported versions may still be precise enough
				params( "0.90.12", true, true, false ),
				params( "5", true, false, false ),
				params( "5.6", true, true, false ),
				params( "5.6.1", true, true, false ),
				// Elasticsearch
				params( "elastic", false, false, false ),
				params( "8", true, false, false ),
				params( "8.9", true, true, false ),
				params( "8.9.1", true, true, false ),
				// OpenSearch
				params( "opensearch", false, false, false ),
				params( "opensearch:2", true, false, false ),
				params( "opensearch:2.9", true, true, false ),
				params( "opensearch:2.9.0", true, true, false ),
				// Amazon OpenSearch Serverless
				params( "amazon-opensearch-serverless", true, true, true ),
				// Technically invalid at the moment, but still precise enough
				params( "amazon-opensearch-serverless:1", true, true, true ),
				params( "amazon-opensearch-serverless:1.0", true, true, true ),
				params( "amazon-opensearch-serverless:1.0.0", true, true, true )
		);
	}

	private static Object[] params(String versionString,
			boolean isPreciseEnoughForBootstrap, boolean isPreciseEnoughForStart, boolean isVersionCheckImpossible) {
		return new Object[] {
				ElasticsearchVersion.of( versionString ),
				isPreciseEnoughForBootstrap,
				isPreciseEnoughForStart,
				isVersionCheckImpossible
		};
	}

	@Parameterized.Parameter
	public ElasticsearchVersion version;
	@Parameterized.Parameter(1)
	public boolean isPreciseEnoughForBootstrap;
	@Parameterized.Parameter(2)
	public boolean isPreciseEnoughForStart;
	@Parameterized.Parameter(3)
	public boolean isVersionCheckImpossible;

	@Test
	public void isPreciseEnoughForBootstrap() {
		assertThat( ElasticsearchDialects.isPreciseEnoughForBootstrap( version ) )
				.isEqualTo( isPreciseEnoughForBootstrap );
	}

	@Test
	public void isPreciseEnoughForStart() {
		assertThat( ElasticsearchDialects.isPreciseEnoughForStart( version ) )
				.isEqualTo( isPreciseEnoughForStart );
	}

	@Test
	public void isVersionCheckImpossible() {
		assertThat( ElasticsearchDialects.isVersionCheckImpossible( version ) )
				.isEqualTo( isVersionCheckImpossible );
	}
}
