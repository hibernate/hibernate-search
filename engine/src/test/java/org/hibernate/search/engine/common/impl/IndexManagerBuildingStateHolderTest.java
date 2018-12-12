/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.testsupport.util.AbstractBeanProviderPartialMock;
import org.hibernate.search.engine.testsupport.util.AbstractConfigurationPropertySourcePartialMock;
import org.hibernate.search.util.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
	private BeanProvider beanProviderMock =
			partialMockBuilder( AbstractBeanProviderPartialMock.class ).mock();

	private IndexManagerBuildingStateHolder holder =
			new IndexManagerBuildingStateHolder( beanProviderMock, configurationSourceMock, rootBuildContextMock );

	@Test
	public void error_missingIndexBackend_emptyOptional() {
		String keyPrefix = "somePrefix.";
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "indexes.indexName.backend" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.resolve( "indexes.indexName.backend" ) )
				.andReturn( Optional.of( keyPrefix + "indexes.indexName.backend" ) );
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.resolve( "default_backend" ) )
				.andReturn( Optional.of( keyPrefix + "default_backend" ) );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Missing backend reference for index 'indexName'" );
		thrown.expectMessage( "Set the property 'somePrefix.indexes.indexName.backend' to a supported value" );
		thrown.expectMessage( "or set 'somePrefix.default_backend' to set a default value for all indexes" );
		holder.startBuilding( "indexName", false );
		verifyAll();
	}

	@Test
	public void error_missingIndexBackend_emptyString() {
		String keyPrefix = "somePrefix.";
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "indexes.indexName.backend" ) )
				.andReturn( (Optional) Optional.of( "" ) );
		EasyMock.expect( configurationSourceMock.resolve( "indexes.indexName.backend" ) )
				.andReturn( Optional.of( keyPrefix + "indexes.indexName.backend" ) );
		EasyMock.expect( configurationSourceMock.get( "default_backend" ) )
				.andReturn( (Optional) Optional.of( "" ) );
		EasyMock.expect( configurationSourceMock.resolve( "default_backend" ) )
				.andReturn( Optional.of( keyPrefix + "default_backend" ) );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Missing backend reference for index 'indexName'" );
		thrown.expectMessage( "Set the property 'somePrefix.indexes.indexName.backend' to a supported value" );
		thrown.expectMessage( "or set 'somePrefix.default_backend' to set a default value for all indexes" );
		holder.startBuilding( "indexName", false );
		verifyAll();
	}

	@Test
	public void error_missingBackendType_emptyOptional() {
		String keyPrefix = "somePrefix.";
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "indexes.indexName.backend" ) )
				.andReturn( (Optional) Optional.of( "backendName" ) );
		EasyMock.expect( configurationSourceMock.get( "backends.backendName.type" ) )
				.andReturn( Optional.empty() );
		EasyMock.expect( configurationSourceMock.resolve( "backends.backendName.type" ) )
				.andReturn( Optional.of( keyPrefix + "backends.backendName.type" ) );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Missing backend type for backend 'backendName'" );
		thrown.expectMessage( "Set the property 'somePrefix.backends.backendName.type' to a supported value" );
		holder.startBuilding( "indexName", false );
		verifyAll();
	}

	@Test
	public void error_missingBackendType_emptyString() {
		String keyPrefix = "somePrefix.";
		resetAll();
		EasyMock.expect( configurationSourceMock.get( "indexes.indexName.backend" ) )
				.andReturn( (Optional) Optional.of( "backendName" ) );
		EasyMock.expect( configurationSourceMock.get( "backends.backendName.type" ) )
				.andReturn( (Optional) Optional.of( "" ) );
		EasyMock.expect( configurationSourceMock.resolve( "backends.backendName.type" ) )
				.andReturn( Optional.of( keyPrefix + "backends.backendName.type" ) );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Missing backend type for backend 'backendName'" );
		thrown.expectMessage( "Set the property 'somePrefix.backends.backendName.type' to a supported value" );
		holder.startBuilding( "indexName", false );
		verifyAll();
	}

}
