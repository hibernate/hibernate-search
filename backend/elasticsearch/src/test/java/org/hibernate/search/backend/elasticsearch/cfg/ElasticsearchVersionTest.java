/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import org.junit.Test;

import org.assertj.core.api.AbstractBooleanAssert;

public class ElasticsearchVersionTest {

	@Test
	public void exactMatch() {
		assertMatch( "5.6.0", "5.6.0" ).isTrue();
		assertMatch( "5.6.1", "5.6.1" ).isTrue();
		assertMatch( "6.0.0", "6.0.0" ).isTrue();
		assertMatch( "6.0.1", "6.0.1" ).isTrue();
		assertMatch( "6.6.0", "6.6.0" ).isTrue();
		assertMatch( "6.6.2", "6.6.2" ).isTrue();
		assertMatch( "6.7.0", "6.7.0" ).isTrue();
		assertMatch( "6.7.1", "6.7.1" ).isTrue();
		assertMatch( "7.0.0", "7.0.0" ).isTrue();
		assertMatch( "7.0.1", "7.0.1" ).isTrue();
		assertMatch( "7.0.0-beta1", "7.0.0-beta1" ).isTrue();
	}

	@Test
	public void lenientMatch() {
		assertMatch( "5", "5.6.0" ).isTrue();
		assertMatch( "5", "5.6.1" ).isTrue();
		assertMatch( "5", "5.7.1" ).isTrue();
		assertMatch( "5.6", "5.6.0" ).isTrue();
		assertMatch( "5.6", "5.6.1" ).isTrue();
		assertMatch( "6", "6.0.0" ).isTrue();
		assertMatch( "6", "6.0.1" ).isTrue();
		assertMatch( "6", "6.6.0" ).isTrue();
		assertMatch( "6", "6.6.1" ).isTrue();
		assertMatch( "6", "6.7.0" ).isTrue();
		assertMatch( "6", "6.7.1" ).isTrue();
		assertMatch( "6.0", "6.0.0" ).isTrue();
		assertMatch( "6.0", "6.0.1" ).isTrue();
		assertMatch( "6.6", "6.6.0" ).isTrue();
		assertMatch( "6.6", "6.6.1" ).isTrue();
		assertMatch( "6.7", "6.7.0" ).isTrue();
		assertMatch( "6.7", "6.7.1" ).isTrue();
		assertMatch( "7", "7.0.0" ).isTrue();
		assertMatch( "7", "7.0.1" ).isTrue();
		assertMatch( "7", "7.0.0-beta1" ).isTrue();
		assertMatch( "7.0", "7.0.0" ).isTrue();
		assertMatch( "7.0", "7.0.1" ).isTrue();
		assertMatch( "7.0", "7.0.0-beta1" ).isTrue();
	}

	@Test
	public void nonMatching() {
		assertMatch( "5.6.0", "5.5.0" ).isFalse();
		assertMatch( "5.6.0", "5.7.0" ).isFalse();
		assertMatch( "5.6.0", "6.0.0" ).isFalse();
		assertMatch( "5.6", "6.0.0" ).isFalse();
		assertMatch( "5", "6.0.0" ).isFalse();
		assertMatch( "6.0.0", "5.6.0" ).isFalse();
		assertMatch( "6.7.0", "6.6.0" ).isFalse();
		assertMatch( "6.6.0", "6.7.0" ).isFalse();
		assertMatch( "6.0.0", "7.0.0" ).isFalse();
		assertMatch( "6.6.0", "7.0.0" ).isFalse();
		assertMatch( "6.0", "5.6.0" ).isFalse();
		assertMatch( "6.0", "7.0.0" ).isFalse();
		assertMatch( "6.0", "6.5.0" ).isFalse();
		assertMatch( "6.0", "6.6.0" ).isFalse();
		assertMatch( "6.6", "5.6.0" ).isFalse();
		assertMatch( "6.6", "7.0.0" ).isFalse();
		assertMatch( "6.6", "6.0.0" ).isFalse();
		assertMatch( "6.6", "6.5.0" ).isFalse();
		assertMatch( "6", "5.6.0" ).isFalse();
		assertMatch( "6", "7.0.0" ).isFalse();
		assertMatch( "7.0.0", "6.6.0" ).isFalse();
		assertMatch( "7.0.0", "5.6.0" ).isFalse();
		assertMatch( "7.0.0", "7.0.1" ).isFalse();
		assertMatch( "7.0.0", "7.1.0" ).isFalse();
		assertMatch( "7.0", "5.6.0" ).isFalse();
		assertMatch( "7.0", "6.0.0" ).isFalse();
		assertMatch( "7.0", "6.5.0" ).isFalse();
		assertMatch( "7.0", "6.6.0" ).isFalse();
		assertMatch( "7.0", "7.1.0" ).isFalse();
		assertMatch( "7", "5.6.0" ).isFalse();
		assertMatch( "7", "6.6.0" ).isFalse();
		assertMatch( "7.0.0", "6.6.0" ).isFalse();
		assertMatch( "7.0.0-beta1", "5.6.0" ).isFalse();
		assertMatch( "7.0.0-beta1", "6.0.0" ).isFalse();
		assertMatch( "7.0.0-beta1", "7.0.0" ).isFalse();
	}

	private AbstractBooleanAssert<?> assertMatch(String configuredVersionString, String actualVersionString) {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( actualVersionString );

		return assertThat( parsedConfiguredVersion.matches( parsedActualVersion ) )
				.as( "ESVersion(" + configuredVersionString + ").matches( ESVersion(" + actualVersionString + ") )" );
	}
}
