/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isVersion;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.jupiter.api.Test;

class ElasticsearchTestDialectVersionTest {

	@Test
	void isBetween() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:6.7.0" ),
						es -> es.isBetween( "6.7", "6.9" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isBetween( "1.0.1", "1.1.2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isBetween( "1.1.1", "1.1.2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						es -> es.isBetween( "1.1.1", "1.1.2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						es -> es.isBetween( "1.1", "2.2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						es -> es.isBetween( "1", "2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();
	}

	@Test
	void anyNone() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> false,
						os -> true,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> false,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "amazon-opensearch-serverless" ),
						es -> true,
						os -> true,
						aoss -> false
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> true,
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> false,
						os -> true,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "amazon-opensearch-serverless" ),
						es -> false,
						os -> false,
						aoss -> true
				)
		).isTrue();
	}

	@Test
	void isMatching() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1.2" ),
						os -> true,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1.1" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1" ),
						os -> false,
						aoss -> false
				)
		).isTrue();
	}

	@Test
	void isAtMost() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> true,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> os.isAtMost( "1.1.0" ),
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> false,
						aoss -> false
				)
		).isTrue();
	}

	@Test
	void isLessThan() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isLessThan( "1.1.2" ),
						os -> false,
						aoss -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> os.isLessThan( "1.1.0" ),
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1" ),
						es -> es.isLessThan( "1.1.0" ),
						os -> true,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isLessThan( "1.0.1" ),
						os -> true,
						aoss -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						es -> es.isLessThan( "1.1.1" ),
						os -> false,
						aoss -> false
				)
		).isTrue();
	}

	@Test
	void amazonOpenSearchServerless_defaultToOpenSearchPredicate() {
		ElasticsearchVersion version = ElasticsearchVersion.of( "amazon-opensearch-serverless" );
		assertThat( isVersion(
				version,
				es -> true,
				os -> os.isAtMost( "2.0" ),
				null
		) ).isFalse();
		assertThat( isVersion(
				version,
				es -> false,
				os -> !os.isAtMost( "2.0" ),
				null
		) ).isTrue();

		assertThat( isVersion(
				version,
				es -> true,
				os -> os.isLessThan( "2.0" ),
				null
		) ).isFalse();
		assertThat( isVersion(
				version,
				es -> false,
				os -> !os.isLessThan( "2.0" ),
				null
		) ).isTrue();

		assertThat( isVersion(
				version,
				es -> true,
				os -> os.isBetween( "2.0", "2.5" ),
				null
		) ).isFalse();
		assertThat( isVersion(
				version,
				es -> false,
				os -> !os.isBetween( "2.0", "2.5" ),
				null
		) ).isTrue();
	}

}
