/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;

@RunWith(EasyMockRunner.class)
public class ElasticsearchDialectFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private ElasticsearchDialectFactory dialectFactory = new ElasticsearchDialectFactory();

	@Mock
	private ElasticsearchClient clientMock;

	@Test
	public void es0() throws Exception {
		doMock( "0.90.12" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'0.90.12'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es10() throws Exception {
		doMock( "1.0.0" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'1.0.0'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es20() throws Exception {
		doMock( "2.0.0" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'2.0.0'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es24() throws Exception {
		doMock( "2.4.4" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'2.4.4'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es50() throws Exception {
		doMock( "5.0.0" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'5.0.0'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es52() throws Exception {
		doMock( "5.2.0" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'5.2.0'" );
		dialectFactory.createFromClusterVersion( clientMock );
	}

	@Test
	public void es56() throws Exception {
		testSuccess( "5.6.12", Elasticsearch56Dialect.class );
	}

	@Test
	public void es60() throws Exception {
		testSuccess( "6.0.0", Elasticsearch6Dialect.class );
	}

	@Test
	public void es66() throws Exception {
		testSuccess( "6.6.0", Elasticsearch6Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es700beta1() throws Exception {
		testSuccess( "7.0.0-beta1", Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3490")
	public void es70() throws Exception {
		testSuccess( "7.0.0", Elasticsearch7Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2748")
	public void es80() throws Exception {
		doMock( "8.0.0" );
		logged.expectMessage( "HSEARCH400085", "'8.0.0'" );
		ElasticsearchDialect dialect = dialectFactory.createFromClusterVersion( clientMock );
		assertThat( dialect ).isInstanceOf( Elasticsearch7Dialect.class );
	}

	private void testSuccess(String versionString, Class<?> expectedDialectClass) throws Exception {
		doMock( versionString );
		logged.expectMessage( "HSEARCH400085" ).never();
		ElasticsearchDialect dialect = dialectFactory.createFromClusterVersion( clientMock );
		assertThat( dialect ).isInstanceOf( expectedDialectClass );
	}

	private void doMock(String versionString) throws Exception {
		JsonObject versionObject = new JsonObject();
		versionObject.addProperty( "number", versionString );
		JsonObject responseBody = new JsonObject();
		responseBody.add( "version", versionObject );
		expect( clientMock.submit( EasyMock.anyObject() ) )
				.andReturn( CompletableFuture.completedFuture( new ElasticsearchResponse( 200, "", responseBody ) ) );
		replay( clientMock );
	}

}
