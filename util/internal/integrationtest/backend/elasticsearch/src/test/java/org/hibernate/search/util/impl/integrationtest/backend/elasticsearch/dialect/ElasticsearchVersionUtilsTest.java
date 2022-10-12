/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.Test;

public class ElasticsearchVersionUtilsTest {

	@Test
	public void isBetween() {
		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:6.7.0" ),
						"elastic:6.7",
						"elastic:6.9"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.0.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						"elastic:1.1.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1.1",
						"elastic:2.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isBetween(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1",
						"elastic:2"
				)
		).isTrue();
	}

	@Test
	public void isOpenSearch() {
		assertThat(
				ElasticsearchVersionUtils.isOpenSearch(
						ElasticsearchVersion.of( "elastic:1.1.1" )
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isOpenSearch(
						ElasticsearchVersion.of( "opensearch:1.1.1" )
				)
		).isTrue();
	}

	@Test
	public void isMatching() {
		assertThat(
				ElasticsearchVersionUtils.isMatching(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.2"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isMatching(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isMatching(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1"
				)
		).isTrue();
	}

	@Test
	public void isAtMost() {
		assertThat(
				ElasticsearchVersionUtils.isAtMost(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						"elastic:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isAtMost(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"opensearch:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isAtMost(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isAtMost(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						"elastic:1.1.1"
				)
		).isTrue();
	}

	@Test
	public void isLessThan() {
		assertThat(
				ElasticsearchVersionUtils.isLessThan(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isLessThan(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"opensearch:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isLessThan(
						ElasticsearchVersion.of( "elastic:1.1" ),
						"elastic:1.1.0"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isLessThan(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.0.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isLessThan(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						"elastic:1.1.1"
				)
		).isTrue();
	}

}
