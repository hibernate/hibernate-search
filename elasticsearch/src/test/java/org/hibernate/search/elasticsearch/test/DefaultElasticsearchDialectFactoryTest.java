/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.dialect.impl.DefaultElasticsearchDialectFactory;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.dialect.impl.es2.Elasticsearch2Dialect;
import org.hibernate.search.elasticsearch.dialect.impl.es50.Elasticsearch50Dialect;
import org.hibernate.search.elasticsearch.dialect.impl.es52.Elasticsearch52Dialect;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
@RunWith(EasyMockRunner.class)
public class DefaultElasticsearchDialectFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private DefaultElasticsearchDialectFactory dialectFactory = new DefaultElasticsearchDialectFactory();

	@Mock
	private ElasticsearchClient clientMock;

	@Test
	public void es0() throws Exception {
		doMock( "0.90.12" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'0.90.12'" );
		dialectFactory.createDialect( clientMock, new Properties() );
	}

	@Test
	public void es10() throws Exception {
		doMock( "1.0.0" );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400081" );
		thrown.expectMessage( "'1.0.0'" );
		dialectFactory.createDialect( clientMock, new Properties() );
	}

	@Test
	public void es20() throws Exception {
		testSuccess( "2.0.0", Elasticsearch2Dialect.class );
	}

	@Test
	public void es24() throws Exception {
		testSuccess( "2.4.4", Elasticsearch2Dialect.class );
	}

	@Test
	public void es50() throws Exception {
		testSuccess( "5.0.0", Elasticsearch50Dialect.class );
	}

	@Test
	public void es52() throws Exception {
		testSuccess( "5.2.0", Elasticsearch52Dialect.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2748")
	public void es60() throws Exception {
		doMock( "6.0.0" );
		logged.expectMessage( "HSEARCH400085", "'6.0.0'" );
		ElasticsearchDialect dialect = dialectFactory.createDialect( clientMock, new Properties() );
		assertThat( dialect ).isInstanceOf( Elasticsearch52Dialect.class );
	}

	private void testSuccess(String versionString, Class<?> expectedDialectClass) throws Exception {
		doMock( versionString );
		ElasticsearchDialect dialect = dialectFactory.createDialect( clientMock, new Properties() );
		assertThat( dialect ).isInstanceOf( expectedDialectClass );
	}

	private void doMock(String versionString) throws Exception {
		JsonObject responseBody = JsonBuilder.object()
				.add( "version", JsonBuilder.object()
						.addProperty( "number", versionString )
				).build();
		expect( clientMock.submit( EasyMock.anyObject() ) )
				.andReturn( CompletableFuture.completedFuture( new ElasticsearchResponse( 200, "", responseBody ) ) );
		replay( clientMock );
	}

}
