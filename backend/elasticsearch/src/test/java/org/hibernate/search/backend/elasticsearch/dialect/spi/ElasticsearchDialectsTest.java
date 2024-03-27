/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ElasticsearchDialectsTest {

	public static List<? extends Arguments> params() {
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

	private static Arguments params(String versionString,
			boolean isPreciseEnoughForBootstrap, boolean isPreciseEnoughForStart, boolean isVersionCheckImpossible) {
		return Arguments.of(
				ElasticsearchVersion.of( versionString ),
				isPreciseEnoughForBootstrap,
				isPreciseEnoughForStart,
				isVersionCheckImpossible
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isPreciseEnoughForBootstrap(ElasticsearchVersion version, boolean isPreciseEnoughForBootstrap,
			boolean isPreciseEnoughForStart, boolean isVersionCheckImpossible) {
		assertThat( ElasticsearchDialects.isPreciseEnoughForBootstrap( version ) )
				.isEqualTo( isPreciseEnoughForBootstrap );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isPreciseEnoughForStart(ElasticsearchVersion version, boolean isPreciseEnoughForBootstrap,
			boolean isPreciseEnoughForStart, boolean isVersionCheckImpossible) {
		assertThat( ElasticsearchDialects.isPreciseEnoughForStart( version ) )
				.isEqualTo( isPreciseEnoughForStart );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isVersionCheckImpossible(ElasticsearchVersion version, boolean isPreciseEnoughForBootstrap,
			boolean isPreciseEnoughForStart, boolean isVersionCheckImpossible) {
		assertThat( ElasticsearchDialects.isVersionCheckImpossible( version ) )
				.isEqualTo( isVersionCheckImpossible );
	}
}
