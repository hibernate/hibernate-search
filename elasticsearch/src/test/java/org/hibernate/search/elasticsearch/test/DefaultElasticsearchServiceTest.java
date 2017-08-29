/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Properties;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.fest.assertions.StringAssert;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialectFactory;
import org.hibernate.search.elasticsearch.impl.DefaultElasticsearchService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.spi.BuildContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.GsonBuilder;

/**
 * @author Yoann Rodiere
 */
@RunWith(EasyMockRunner.class)
public class DefaultElasticsearchServiceTest {

	@Mock(type = MockType.NICE)
	private BuildContext contextMock;

	@Mock(type = MockType.NICE)
	private ServiceManager serviceManagerMock;

	@Mock
	private ElasticsearchClientFactory clientFactoryMock;

	@Mock(type = MockType.NICE)
	private ElasticsearchClientImplementor clientMock;

	@Mock
	private ElasticsearchDialectFactory dialectFactoryMock;

	@Mock(type = MockType.NICE)
	private ElasticsearchDialect dialectMock;

	private DefaultElasticsearchService service = new DefaultElasticsearchService();

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void propertyMasking() throws Exception {
		Capture<Properties> propertiesCapture = new Capture<>();

		expect( contextMock.getServiceManager() ).andReturn( serviceManagerMock ).anyTimes();
		expect( serviceManagerMock.requestReference( anyObject() ) ).andAnswer(
				() -> new ServiceReference( serviceManagerMock, (Class<?>) getCurrentArguments()[0] )
		).anyTimes();

		expect( serviceManagerMock.requestService( ElasticsearchClientFactory.class ) ).andReturn( clientFactoryMock );
		expect( clientFactoryMock.create( EasyMock.capture( propertiesCapture ) ) ).andReturn( clientMock );

		expect( serviceManagerMock.requestService( ElasticsearchDialectFactory.class ) ).andReturn( dialectFactoryMock );
		expect( dialectFactoryMock.createDialect( anyObject(), anyObject() ) ).andReturn( dialectMock );
		expect( dialectMock.createGsonBuilderBase() ).andAnswer( GsonBuilder::new ).anyTimes();

		replay( contextMock, serviceManagerMock, clientFactoryMock, clientMock, dialectFactoryMock, dialectMock );

		Properties properties = new Properties();
		properties.setProperty( "1", "1" );
		properties.setProperty( "hibernate.search.2", "2" );
		properties.setProperty( "hibernate.search.elasticsearch.3", "3" );
		properties.setProperty( "hibernate.search.default.elasticsearch.4", "4" );

		service.start( properties, contextMock );

		Properties maskedProperties = propertiesCapture.getValue();

		assertProperty( maskedProperties, "2" ).isEqualTo( "2" );
		assertProperty( maskedProperties, "elasticsearch.3" ).isEqualTo( "3" );
		assertProperty( maskedProperties, "elasticsearch.4" ).isEqualTo( "4" );

		for ( String originalName : properties.stringPropertyNames() ) {
			assertProperty( maskedProperties, originalName ).isNull();
		}

		assertProperty( maskedProperties, "3" ).isNull();
		assertProperty( maskedProperties, "4" ).isNull();

		/*
		 * Annoying side-effects of allowing access to hibernate.search.*,
		 * but for now we can't do otherwise.
		 */
		assertProperty( maskedProperties, "default.elasticsearch.4" ).isEqualTo( "4" );
	}

	private StringAssert assertProperty(Properties maskedProperties, String name) {
		return assertThat( maskedProperties.getProperty( name ) ).as( "Property from masked properties '" + name + "'" );
	}

}
