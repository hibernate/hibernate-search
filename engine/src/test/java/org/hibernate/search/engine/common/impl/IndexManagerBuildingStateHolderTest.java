/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

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
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

// We have to use raw types to mock methods returning generic types with wildcards
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IndexManagerBuildingStateHolderTest extends EasyMockSupport {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final RootBuildContext rootBuildContextMock = createMock( RootBuildContext.class );
	private final ConfigurationPropertySource configurationSourceMock =
			partialMockBuilder( AbstractConfigurationPropertySourcePartialMock.class ).mock();
	private final BeanResolver beanResolverMock =
			partialMockBuilder( AbstractBeanResolverPartialMock.class ).mock();

	@Test
	public void defaultBackend_noNameSet() {
		BackendFactory backendFactoryMock = createMock( BackendFactory.class );
		BackendImplementor backendMock = createMock( BackendImplementor.class );
		IndexManagerBuilder indexManagerBuilderMock = createMock( IndexManagerBuilder.class );
		IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilderMock = createMock( IndexSchemaRootNodeBuilder.class );

		Capture<ConfigurationPropertySource> backendPropertySourceCapture = Capture.newInstance();
		Capture<ConfigurationPropertySource> indexPropertySourceCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.empty() );
		replayAll();
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		resetAll();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.type" ) )
				.andReturn( (Optional) Optional.of( "someBackendType" ) );
		EasyMock.expect( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.andReturn( BeanHolder.of( backendFactoryMock ) );
		EasyMock.expect( backendFactoryMock.create(
				EasyMock.eq( EventContexts.defaultBackend() ),
				EasyMock.anyObject(),
				EasyMock.capture( backendPropertySourceCapture )
		) )
				.andReturn( backendMock );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyAll();

		resetAll();
		EasyMock.expect( backendMock.createIndexManagerBuilder(
				EasyMock.eq( "myIndex" ),
				EasyMock.eq( "myType" ),
				EasyMock.eq( false ),
				EasyMock.anyObject(),
				EasyMock.capture( indexPropertySourceCapture )
		) )
				.andReturn( indexManagerBuilderMock );
		EasyMock.expect( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.andStubReturn( indexSchemaRootNodeBuilderMock );
		replayAll();
		holder.getIndexManagerBuildingState(
				Optional.empty(), "myIndex", "myType", false
		);
		verifyAll();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backend.index_defaults.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void defaultBackend_nameSet() {
		BackendFactory backendFactoryMock = createMock( BackendFactory.class );
		BackendImplementor backendMock = createMock( BackendImplementor.class );
		IndexManagerBuilder indexManagerBuilderMock = createMock( IndexManagerBuilder.class );
		IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilderMock = createMock( IndexSchemaRootNodeBuilder.class );

		Capture<ConfigurationPropertySource> backendPropertySourceCapture = Capture.newInstance();
		Capture<ConfigurationPropertySource> indexPropertySourceCapture = Capture.newInstance();

		logged.expectMessage(
				"Using configuration property 'hibernate.search.default_backend' to set the name of the default backend to 'myBackend'.",
				"This configuration property is deprecated and shouldn't be used anymore" );
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.of( "myBackend" ) );
		EasyMock.expect( configurationSourceMock.resolve( "default_backend" ) )
				.andStubReturn( Optional.of( "hibernate.search.default_backend" ) );
		replayAll();
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		resetAll();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.type" ) )
				.andReturn( (Optional) Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.type" ) )
				.andReturn( (Optional) Optional.of( "someBackendType" ) );
		EasyMock.expect( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.andReturn( BeanHolder.of( backendFactoryMock ) );
		EasyMock.expect( backendFactoryMock.create(
				EasyMock.eq( EventContexts.fromBackendName( "myBackend" ) ),
				EasyMock.anyObject(),
				EasyMock.capture( backendPropertySourceCapture )
		) )
				.andReturn( backendMock );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyAll();

		resetAll();
		EasyMock.expect( backendMock.createIndexManagerBuilder(
				EasyMock.eq( "myIndex" ),
				EasyMock.eq( "myType" ),
				EasyMock.eq( false ),
				EasyMock.anyObject(),
				EasyMock.capture( indexPropertySourceCapture )
		) )
				.andReturn( indexManagerBuilderMock );
		EasyMock.expect( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.andStubReturn( indexSchemaRootNodeBuilderMock );
		replayAll();
		holder.getIndexManagerBuildingState(
				Optional.empty(), "myIndex", "myType", false
		);
		verifyAll();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration - syntax "hibernate.search.backend.foo"
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Backend configuration - syntax "hibernate.search.backends.myBackend.foo"
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.foo" ) )
				.andReturn( (Optional) Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.andReturn( (Optional) Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );

		// Index configuration defaults
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.indexes.myIndex.foo" ) )
				.andReturn( (Optional) Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.indexes.myIndex.foo" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backend.index_defaults.foo" ) )
				.andReturn( (Optional) Optional.empty() );
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.index_defaults.foo" ) )
				.andReturn( (Optional) Optional.of( "bar" ) );
		replayAll();
		result = indexPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).contains( "bar" );
	}

	@Test
	public void explicitBackend() {
		BackendFactory backendFactoryMock = createMock( BackendFactory.class );
		BackendImplementor backendMock = createMock( BackendImplementor.class );
		IndexManagerBuilder indexManagerBuilderMock = createMock( IndexManagerBuilder.class );
		IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilderMock = createMock( IndexSchemaRootNodeBuilder.class );

		Capture<ConfigurationPropertySource> backendPropertySourceCapture = Capture.newInstance();
		Capture<ConfigurationPropertySource> indexPropertySourceCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.empty() );
		replayAll();
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		resetAll();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.type" ) )
				.andReturn( (Optional) Optional.of( "someBackendType" ) );
		EasyMock.expect( beanResolverMock.resolve( BackendFactory.class, "someBackendType" ) )
				.andReturn( BeanHolder.of( backendFactoryMock ) );
		EasyMock.expect( backendFactoryMock.create(
				EasyMock.eq( EventContexts.fromBackendName( "myBackend" ) ),
				EasyMock.anyObject(),
				EasyMock.capture( backendPropertySourceCapture )
		) )
				.andReturn( backendMock );
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
				.andReturn( indexManagerBuilderMock );
		EasyMock.expect( indexManagerBuilderMock.schemaRootNodeBuilder() )
				.andStubReturn( indexSchemaRootNodeBuilderMock );
		replayAll();
		holder.getIndexManagerBuildingState(
				Optional.of( "myBackend" ), "myIndex", "myType", false
		);
		verifyAll();

		// Check that configuration property sources behave as expected
		Optional result;

		// Backend configuration - empty
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backends.myBackend.foo" ) )
				.andReturn( (Optional) Optional.empty() );
		replayAll();
		result = backendPropertySourceCapture.getValue().get( "foo" );
		verifyAll();
		assertThat( result ).isEmpty();

		// Backend configuration - non-empty
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
	public void error_missingBackendType_nullType() {
		String keyPrefix = "somePrefix.";

		FailureCollector rootFailureCollectorMock = createMock( FailureCollector.class );
		ContextualFailureCollector backendFailureCollectorMock = createMock( ContextualFailureCollector.class );

		Capture<Throwable> throwableCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.empty() );
		replayAll();
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		resetAll();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.type" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( beanResolverMock.namedConfiguredForRole( BackendFactory.class ) )
				.andReturn( Collections.emptyMap() );
		EasyMock.expect( configurationSourceMock.resolve( "backend.type" ) )
				.andReturn( Optional.of( keyPrefix + "backend.type" ) );
		EasyMock.expect( rootBuildContextMock.getFailureCollector() )
				.andReturn( rootFailureCollectorMock );
		EasyMock.expect( rootFailureCollectorMock.withContext( EventContexts.defaultBackend() ) )
				.andReturn( backendFailureCollectorMock );
		backendFailureCollectorMock.add( EasyMock.capture( throwableCapture ) );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyAll();

		assertThat( throwableCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Configuration property 'somePrefix.backend.type' is not set, and no backend was found in the classpath",
						"Did you forget to add the desired backend to your project's dependencies?" );
	}

	@Test
	public void error_missingBackendType_emptyType() {
		String keyPrefix = "somePrefix.";

		FailureCollector rootFailureCollectorMock = createMock( FailureCollector.class );
		ContextualFailureCollector backendFailureCollectorMock = createMock( ContextualFailureCollector.class );

		Capture<Throwable> throwableCapture = Capture.newInstance();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.empty() );
		replayAll();
		IndexManagerBuildingStateHolder holder =
				new IndexManagerBuildingStateHolder( beanResolverMock, configurationSourceMock, rootBuildContextMock );
		resetAll();

		resetAll();
		EasyMock.expect( configurationSourceMock.get( "backend.type" ) )
				.andReturn( (Optional) Optional.of( "" ) );
		EasyMock.expect( beanResolverMock.namedConfiguredForRole( BackendFactory.class ) )
				.andReturn( Collections.emptyMap() );
		EasyMock.expect( configurationSourceMock.resolve( "backend.type" ) )
				.andReturn( Optional.of( keyPrefix + "backend.type" ) );
		EasyMock.expect( rootBuildContextMock.getFailureCollector() )
				.andReturn( rootFailureCollectorMock );
		EasyMock.expect( rootFailureCollectorMock.withContext( EventContexts.defaultBackend() ) )
				.andReturn( backendFailureCollectorMock );
		backendFailureCollectorMock.add( EasyMock.capture( throwableCapture ) );
		replayAll();
		holder.createBackends( CollectionHelper.asSet( Optional.empty() ) );
		verifyAll();

		assertThat( throwableCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Configuration property 'somePrefix.backend.type' is not set, and no backend was found in the classpath",
						"Did you forget to add the desired backend to your project's dependencies?" );
	}

}
