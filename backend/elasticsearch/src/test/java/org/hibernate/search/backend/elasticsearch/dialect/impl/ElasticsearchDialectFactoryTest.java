/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch56ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch6ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch56ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch60ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch67ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
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
	public void es09012() {
		testUnsupported( "0.90.12" );
	}

	@Test
	public void es100() {
		testUnsupported( "1.0.0" );
	}

	@Test
	public void es200() {
		testUnsupported( "2.0.0" );
	}

	@Test
	public void es244() {
		testUnsupported( "2.4.4" );
	}

	@Test
	public void es500() {
		testUnsupported( "5.0.0" );
	}

	@Test
	public void es520() {
		testUnsupported( "5.2.0" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es5() {
		testAmbiguous( "5" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es56() {
		testSuccess(
				"5.6", "5.6.12",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	public void es5612() {
		testSuccess(
				"5.6.12", "5.6.12",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	public void es570() {
		testSuccessWithWarning(
				"5.7.0", "5.7.0",
				Elasticsearch56ModelDialect.class, Elasticsearch56ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es6() {
		testSuccess(
				"6", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
		testSuccess(
				"6", "6.6.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
		testSuccess(
				"6", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es60() {
		testSuccess(
				"6.0", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	public void es600() {
		testSuccess(
				"6.0.0", "6.0.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es66() {
		testSuccess(
				"6.6", "6.6.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	public void es660() {
		testSuccess(
				"6.6.0", "6.6.0",
				Elasticsearch6ModelDialect.class, Elasticsearch60ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es67() {
		testSuccess(
				"6.7", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void es670() {
		testSuccess(
				"6.7.0", "6.7.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void es680() {
		testSuccess(
				"6.8.0", "6.8.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	public void es690() {
		testSuccessWithWarning(
				"6.9.0", "6.9.0",
				Elasticsearch6ModelDialect.class, Elasticsearch67ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es700beta1() {
		testSuccess(
				"7.0.0-beta1", "7.0.0-beta1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es7() {
		testSuccess(
				"7", "7.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es70() {
		testSuccess(
				"7.0", "7.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es700() {
		testSuccess(
				"7.0.0", "7.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3592")
	public void es71() {
		testSuccess(
				"7.1", "7.1.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3592")
	public void es711() {
		testSuccess(
				"7.1.1", "7.1.1",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3625")
	public void es72() {
		testSuccess(
				"7.2", "7.2.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3625")
	public void es720() {
		testSuccess(
				"7.2.0", "7.2.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3660")
	public void es73() {
		testSuccess(
				"7.3", "7.3.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3660")
	public void es730() {
		testSuccess(
				"7.3.0", "7.3.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3723")
	public void es74() {
		testSuccess(
				"7.4", "7.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3723")
	public void es740() {
		testSuccess(
				"7.4.0", "7.4.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es8() {
		testSuccessWithWarning(
				"8", "8.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3563")
	public void es80() {
		testSuccessWithWarning(
				"8.0", "8.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2748")
	public void es800() {
		testSuccessWithWarning(
				"8.0.0", "8.0.0",
				Elasticsearch7ModelDialect.class, Elasticsearch70ProtocolDialect.class
		);
	}

	private void testUnsupported(String unsupportedVersionString) {
		SubTest.expectException(
				"Test unsupported version " + unsupportedVersionString,
				() -> {
					dialectFactory.createModelDialect( ElasticsearchVersion.of( unsupportedVersionString ) );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400081" )
				.hasMessageContaining( "'" + unsupportedVersionString + "'" );
	}

	private void testAmbiguous(String versionString) {
		SubTest.expectException(
				"Test ambiguous version " + versionString,
				() -> {
					dialectFactory.createModelDialect( ElasticsearchVersion.of( versionString ) );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400561" )
				.hasMessageContaining( "Ambiguous Elasticsearch version: '" + versionString + "'." )
				.hasMessageContaining( "Please use a more precise version to remove the ambiguity" );
	}

	private void testSuccessWithWarning(String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( actualVersionString );

		logged.expectMessage( "HSEARCH400085", "'" + parsedActualVersion + "'" );

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

	private void testSuccess(String configuredVersionString, String actualVersionString,
			Class<? extends ElasticsearchModelDialect> expectedModelDialectClass,
			Class<? extends ElasticsearchProtocolDialect> expectedProtocolDialectClass) {
		ElasticsearchVersion parsedConfiguredVersion = ElasticsearchVersion.of( configuredVersionString );
		ElasticsearchVersion parsedActualVersion = ElasticsearchVersion.of( actualVersionString );

		logged.expectMessage( "HSEARCH400085" ).never();

		ElasticsearchModelDialect modelDialect = dialectFactory.createModelDialect( parsedConfiguredVersion );
		assertThat( modelDialect ).isInstanceOf( expectedModelDialectClass );

		ElasticsearchProtocolDialect protocolDialect = dialectFactory.createProtocolDialect( parsedActualVersion );
		assertThat( protocolDialect ).isInstanceOf( expectedProtocolDialectClass );
	}

}
