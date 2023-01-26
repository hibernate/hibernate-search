/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isVersion;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticsearchTestDialectVersionTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version", "elastic:1.1.1" );
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty( "org.hibernate.search.integrationtest.backend.elasticsearch.version" );
	}

	@Test
	public void isBetween() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:6.7.0" ),
						es -> es.isBetween( "6.7", "6.9" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isBetween( "1.0.1", "1.1.2" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isBetween( "1.1.1", "1.1.2" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						es -> es.isBetween( "1.1.1", "1.1.2" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						es -> es.isBetween( "1.1", "2.2" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:2.2.2" ),
						es -> es.isBetween( "1", "2" ),
						os -> false
				)
		).isTrue();
	}

	@Test
	public void anyNone() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> false,
						os -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> false
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> true,
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> false,
						os -> true
				)
		).isTrue();
	}

	@Test
	public void isMatching() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1.2" ),
						os -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1.1" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isMatching( "1.1" ),
						os -> false
				)
		).isTrue();
	}

	@Test
	public void isAtMost() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.2" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> os.isAtMost( "1.1.0" )
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						es -> es.isAtMost( "1.1.1" ),
						os -> false
				)
		).isTrue();
	}

	@Test
	public void isLessThan() {
		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isLessThan( "1.1.2" ),
						os -> false
				)
		).isTrue();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "opensearch:1.1.1" ),
						es -> true,
						os -> os.isLessThan( "1.1.0" )
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1" ),
						es -> es.isLessThan( "1.1.0" ),
						os -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.1.1" ),
						es -> es.isLessThan( "1.0.1" ),
						os -> true
				)
		).isFalse();

		assertThat(
				isVersion(
						ElasticsearchVersion.of( "elastic:1.0.1" ),
						es -> es.isLessThan( "1.1.1" ),
						os -> false
				)
		).isTrue();
	}

}
