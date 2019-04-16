/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchDialectFactoryTest {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();

	@Test
	public void es0() {
		testUnsupported( "0.90.12" );
	}

	@Test
	public void es10() {
		testUnsupported( "1.0.0" );
	}

	@Test
	public void es20() {
		testUnsupported( "2.0.0" );
	}

	@Test
	public void es24() {
		testUnsupported( "2.4.4" );
	}

	@Test
	public void es50() {
		testUnsupported( "5.0.0" );
	}

	@Test
	public void es52() {
		testUnsupported( "5.2.0" );
	}

	@Test
	public void es56() {
		testSuccess( "5.6.12", Elasticsearch56Dialect.class );
	}

	@Test
	public void es57() {
		testSuccessWithWarning( "5.7.0", Elasticsearch56Dialect.class );
	}

	@Test
	public void es60() {
		testSuccess( "6.0.0", Elasticsearch60Dialect.class );
	}

	@Test
	public void es66() {
		testSuccess( "6.6.0", Elasticsearch60Dialect.class );
	}

	@Test
	public void es67() {
		testSuccess( "6.7.0", Elasticsearch67Dialect.class );
	}

	@Test
	public void es68() {
		testSuccessWithWarning( "6.8.0", Elasticsearch67Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es700beta1() {
		testSuccess( "7.0.0-beta1", Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es70() {
		testSuccess( "7.0.0", Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2748")
	public void es80() {
		testSuccessWithWarning( "8.0.0", Elasticsearch7Dialect.class );
	}

	private void testUnsupported(String unsupportedVersionString) {
		SubTest.expectException(
				"Test unsupported version " + unsupportedVersionString,
				() -> {
					dialectFactory.create( ElasticsearchVersion.of( unsupportedVersionString ) );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400081" )
				.hasMessageContaining( "'" + unsupportedVersionString + "'" );
	}

	private void testSuccessWithWarning(String versionString, Class<?> expectedDialectClass) {
		ElasticsearchVersion parsedVersion = ElasticsearchVersion.of( versionString );

		logged.expectMessage( "HSEARCH400085", "'" + versionString + "'" );
		ElasticsearchDialect dialect = dialectFactory.create( parsedVersion );
		assertThat( dialect ).isInstanceOf( expectedDialectClass );
	}

	private void testSuccess(String versionString, Class<?> expectedDialectClass) {
		ElasticsearchVersion parsedVersion = ElasticsearchVersion.of( versionString );

		logged.expectMessage( "HSEARCH400085" ).never();
		ElasticsearchDialect dialect = dialectFactory.create( parsedVersion );
		assertThat( dialect ).isInstanceOf( expectedDialectClass );
	}

}
