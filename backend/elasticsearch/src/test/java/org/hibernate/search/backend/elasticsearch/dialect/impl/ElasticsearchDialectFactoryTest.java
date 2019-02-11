/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchDialectName;
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
	public void inappropriate() {
		SubTest.expectException(
				() -> {
					dialectFactory.checkAppropriate( ElasticsearchDialectName.ES_6, ElasticsearchVersion.of( "5.6.0" ) );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unexpected Elasticsearch version" )
				.hasMessageContaining( "'5.6.0'" )
				.hasMessageContaining( ElasticsearchDialectName.ES_5_6.toString() )
				.hasMessageContaining( ElasticsearchDialectName.ES_6.toString() );
	}

	@Test
	public void es0() throws Exception {
		testUnsupported( "0.90.12" );
	}

	@Test
	public void es10() throws Exception {
		testUnsupported( "1.0.0" );
	}

	@Test
	public void es20() throws Exception {
		testUnsupported( "2.0.0" );
	}

	@Test
	public void es24() throws Exception {
		testUnsupported( "2.4.4" );
	}

	@Test
	public void es50() throws Exception {
		testUnsupported( "5.0.0" );
	}

	@Test
	public void es52() throws Exception {
		testUnsupported( "5.2.0" );
	}

	@Test
	public void es56() {
		testSuccess( "5.6.12", ElasticsearchDialectName.ES_5_6, Elasticsearch56Dialect.class );
	}

	@Test
	public void es57() {
		logged.expectMessage( "HSEARCH400085", "'5.7.0'" );
		ElasticsearchDialectName dialectName = dialectFactory.getAppropriateDialectName( ElasticsearchVersion.of( "5.7.0" ) );
		assertThat( dialectName ).isEqualTo( ElasticsearchDialectName.ES_5_6 );
	}

	@Test
	public void es60() {
		testSuccess( "6.0.0", ElasticsearchDialectName.ES_6, Elasticsearch6Dialect.class );
	}

	@Test
	public void es66() {
		testSuccess( "6.6.0", ElasticsearchDialectName.ES_6, Elasticsearch6Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es700beta1() throws Exception {
		testSuccess( "7.0.0-beta1", ElasticsearchDialectName.ES_7, Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es70() throws Exception {
		testSuccess( "7.0.0", ElasticsearchDialectName.ES_7, Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2748")
	public void es80() throws Exception {
		logged.expectMessage( "HSEARCH400085", "'8.0.0'" );
		ElasticsearchDialectName dialectName = dialectFactory.getAppropriateDialectName( ElasticsearchVersion.of( "8.0.0" ) );
		assertThat( dialectName ).isEqualTo( ElasticsearchDialectName.ES_7 );
	}

	private void testUnsupported(String unsupportedVersionString) {
		for ( ElasticsearchDialectName name : ElasticsearchDialectName.values() ) {
			SubTest.expectException(
					"Test with unsupported version " + unsupportedVersionString + " with dialect name " + name,
					() -> {
						dialectFactory.getAppropriateDialectName( ElasticsearchVersion.of( unsupportedVersionString ) );
					}
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "HSEARCH400081" )
					.hasMessageContaining( "'" + unsupportedVersionString + "'" );
		}
	}

	private void testSuccess(String versionString, ElasticsearchDialectName expectedDialectName, Class<?> expectedDialectClass) {
		ElasticsearchVersion parsedVersion = ElasticsearchVersion.of( versionString );

		ElasticsearchDialectName dialectName = dialectFactory.getAppropriateDialectName( parsedVersion );
		assertThat( dialectName ).isEqualTo( expectedDialectName );

		logged.expectMessage( "HSEARCH400085" ).never();
		ElasticsearchDialect dialect = dialectFactory.create( dialectName );
		assertThat( dialect ).isInstanceOf( expectedDialectClass );

		// Should not throw an exception
		dialectFactory.checkAppropriate( expectedDialectName, parsedVersion );
	}

}
