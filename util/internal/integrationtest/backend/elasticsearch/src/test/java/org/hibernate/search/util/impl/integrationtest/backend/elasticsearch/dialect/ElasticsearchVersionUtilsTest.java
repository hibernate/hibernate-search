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
	public void isActualVersionBetween() {
		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetween(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.0.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetween(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1",
						"elastic:3"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetween(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1.1",
						"elastic:3.1"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetween(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1.1",
						"elastic:2.2"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetween(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1",
						"elastic:1.1.2"
				)
		).isFalse();
	}

	@Test
	public void isActualVersionBetweenIncluding() {
		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetweenIncluding(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.0.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetweenIncluding(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetweenIncluding(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						"elastic:1.1.1",
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetweenIncluding(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1.1",
						"elastic:2.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionBetweenIncluding(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						"elastic:1",
						"elastic:2"
				)
		).isTrue();
	}

	@Test
	public void isOpensearchDistribution() {
		assertThat(
				ElasticsearchVersionUtils.isOpensearchDistribution(
						ElasticsearchVersion.of( "elastic:1.1.1" )
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isOpensearchDistribution(
						ElasticsearchVersion.of( "opensearch:1.1.1" )
				)
		).isTrue();
	}

	@Test
	public void isActualVersionEquals() {
		assertThat(
				ElasticsearchVersionUtils.isActualVersionEquals(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.2"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionEquals(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionEquals(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1"
				)
		).isTrue();
	}

	@Test
	public void isActualVersionLessOrEquals() {
		assertThat(
				ElasticsearchVersionUtils.isActualVersionLessOrEquals(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						"elastic:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLessOrEquals(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"opensearch:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLessOrEquals(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.1"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLessOrEquals(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						"elastic:1.1.1"
				)
		).isTrue();
	}

	@Test
	public void isActualVersionLess() {
		assertThat(
				ElasticsearchVersionUtils.isActualVersionLess(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.1.2"
				)
		).isTrue();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLess(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"opensearch:1.1.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLess(
						ElasticsearchVersion.of( "elastic:1.1" ),
						"elastic:1.1.0"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLess(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						"elastic:1.0.1"
				)
		).isFalse();

		assertThat(
				ElasticsearchVersionUtils.isActualVersionLess(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						"elastic:1.1.1"
				)
		).isTrue();
	}

}
