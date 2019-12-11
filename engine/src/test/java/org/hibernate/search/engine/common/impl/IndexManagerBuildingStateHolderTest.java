/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.testsupport.util.AbstractBeanResolverPartialMock;
import org.hibernate.search.engine.testsupport.util.AbstractConfigurationPropertySourcePartialMock;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

// We have to use raw types to mock methods returning generic types with wildcards
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IndexManagerBuildingStateHolderTest extends EasyMockSupport {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private RootBuildContext rootBuildContextMock = createMock( RootBuildContext.class );
	private ConfigurationPropertySource configurationSourceMock =
			partialMockBuilder( AbstractConfigurationPropertySourcePartialMock.class ).mock();
	private BeanResolver beanResolverMock =
			partialMockBuilder( AbstractBeanResolverPartialMock.class ).mock();

	private IndexManagerBuildingStateHolder holder =
			new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );

	@Test
	public void success() {
		BackendFactory backendFactoryMock = createMock( BackendFactory.class );
		BackendImplementor<DocumentElement> backendMock = createMock( BackendImplementor.class );
		IndexManagerBuilder<DocumentElement> indexManagerBuilderMock = createMock( IndexManagerBuilder.class );
		IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilderMock = createMock( IndexSchemaRootNodeBuilder.class );

		Capture<ConfigurationPropertySource> backendPropertySourceCapture = Capture.newInstance();
		Capture<ConfigurationPropertySource> indexPropertySourceCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.type" ) )
				.andReturn( (Optional) Optional.of( "someBackendType" ) );
		EasyMock.expect( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.andReturn( BeanHolder.of( backendFactoryMock ) );
		EasyMock.expect( backendFactoryMock.create(
				EasyMock.eq( "myBackend" ),
				EasyMock.anyObject(),
				EasyMock.capture( backendPropertySourceCapture )
		) )
				.andReturn( (BackendImplementor) backendMock );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.of( "myBackend" ) ) );
		verifyAll();

		resetAll();
		EasyMock.expect( backendMock.createIndexManagerBuilder(
				EasyMock.eq( "myIndex" ),
				EasyMock.eq( "myType" ),
				EasyMock.eq( false ),
				EasyMock.anyObject(),
				EasyMock.capture( indexPropertySourceCapture )
		) )
				.andReturn( (IndexManagerBuilder) indexManagerBuilderMock );
		EasyMock.expect( indexManagerBuilderMock.getSchemaRootNodeBuilder() )
				.andStubReturn( indexSchemaRootNodeBuilderMock );
		replayAll();
		holder.getIndexManagerBuildingState(
				Optional.of( "myBackend" ), "myIndex", "myType", false
		);
		verifyAll();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.index_defaults.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void error_missingBackend_emptyName() {
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.type" ) )
				.andReturn( (Optional) Optional.of( "someBackendType" ) );
		String keyPrefix = "somePrefix.";
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.resolve( "default_backend" ) )
				.andReturn( Optional.of( keyPrefix + "default_backend" ) );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "The name of the default backend is not set" );
		thrown.expectMessage( "Set it through the configuration property 'somePrefix.default_backend'" );
		thrown.expectMessage( "or set the backend name explicitly for each indexed type in your mapping" );
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyAll();
	}

	@Test
	public void error_missingBackendType_nullType() {
		String keyPrefix = "somePrefix.";

		FailureCollector rootFailureCollectorMock = createMock( FailureCollector.class );
		ContextualFailureCollector backendFailureCollectorMock = createMock( ContextualFailureCollector.class );

		Capture<Throwable> throwableCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.backendName.type" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.resolve( "backends.backendName.type" ) )
				.andReturn( Optional.of( keyPrefix + "backends.backendName.type" ) );
		EasyMock.expect( rootBuildContextMock.getFailureCollector() )
				.andReturn( rootFailureCollectorMock );
		EasyMock.expect( rootFailureCollectorMock.withContext( EventContexts.fromBackendName( "backendName" ) ) )
				.andReturn( backendFailureCollectorMock );
		backendFailureCollectorMock.add( EasyMock.capture( throwableCapture ) );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.of( "backendName" ) ) );
		verifyAll();

		assertThat( throwableCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Missing backend type for backend 'backendName'" )
				.hasMessageContaining( "Set the property 'somePrefix.backends.backendName.type' to a supported value" );
	}

	@Test
	public void error_missingBackendType_emptyType() {
		String keyPrefix = "somePrefix.";

		FailureCollector rootFailureCollectorMock = createMock( FailureCollector.class );
		ContextualFailureCollector backendFailureCollectorMock = createMock( ContextualFailureCollector.class );

		Capture<Throwable> throwableCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.backendName.type" ) )
				.andReturn( (Optional) Optional.of( "" ) );
		EasyMock.expect( configurationSourceMock.resolve( "backends.backendName.type" ) )
				.andReturn( Optional.of( keyPrefix + "backends.backendName.type" ) );
		EasyMock.expect( rootBuildContextMock.getFailureCollector() )
				.andReturn( rootFailureCollectorMock );
		EasyMock.expect( rootFailureCollectorMock.withContext( EventContexts.fromBackendName( "backendName" ) ) )
				.andReturn( backendFailureCollectorMock );
		backendFailureCollectorMock.add( EasyMock.capture( throwableCapture ) );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.of( "backendName" ) ) );
		verifyAll();

		assertThat( throwableCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Missing backend type for backend 'backendName'" )
				.hasMessageContaining( "Set the property 'somePrefix.backends.backendName.type' to a supported value" );
	}

}
