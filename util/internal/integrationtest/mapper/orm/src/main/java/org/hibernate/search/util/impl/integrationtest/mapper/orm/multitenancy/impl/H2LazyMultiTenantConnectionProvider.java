/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import static org.hibernate.testing.env.ConnectionProviderBuilder.URL_FORMAT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.env.ConnectionProviderBuilder;

import org.jboss.logging.Logger;

public class H2LazyMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider<Object>
		implements Stoppable {

	private static final Logger log = Logger.getLogger( H2LazyMultiTenantConnectionProvider.class.getName() );

	private final Object[] tenantIds;
	private final Map<Object, ConnectionProvider> delegates = new HashMap<>();

	public H2LazyMultiTenantConnectionProvider(Object[] tenantIds) {
		this.tenantIds = tenantIds;
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			for ( ConnectionProvider connectionProvider : delegates.values() ) {
				if ( connectionProvider instanceof Stoppable ) {
					closer.push( Stoppable::stop, ( (Stoppable) connectionProvider ) );
				}
				else {
					log.warn( "Connection provider " + connectionProvider
							+ " does not implement Stoppable. This provider will not be stopped." );
				}
			}
		}
	}

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		//blatantly assuming there's at least one entry:
		return getOrCreateDelegates().entrySet().iterator().next().getValue();
	}

	@Override
	public ConnectionProvider selectConnectionProvider(Object tenantIdentifier) {
		ConnectionProvider connectionProviderImpl = getOrCreateDelegates().get( tenantIdentifier );
		if ( connectionProviderImpl == null ) {
			throw new HibernateException( "Unknown tenant identifier" );
		}
		return connectionProviderImpl;
	}

	@SuppressWarnings("deprecation")
	private Map<Object, ConnectionProvider> getOrCreateDelegates() {
		if ( !delegates.isEmpty() ) {
			return delegates;
		}

		Properties properties = Environment.getProperties();
		HashMap<String, Object> map = new HashMap<>();
		DatabaseContainer.configuration().add( map );
		// properties should be a copy, so we edit it as we want!
		properties.putAll( map );

		try {
			Method buildConnectionProvider = ConnectionProviderBuilder.class.getDeclaredMethod( "buildConnectionProvider",
					Properties.class, boolean.class );
			buildConnectionProvider.setAccessible( true );

			for ( Object tenantId : tenantIds ) {
				properties.put( JdbcSettings.URL, String.format( Locale.ROOT, URL_FORMAT, tenantId ) );
				ConnectionProvider connectionProvider =
						(ConnectionProvider) buildConnectionProvider.invoke( null, properties, false );
				delegates.put( tenantId, connectionProvider );
			}
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException( "Unable to prepare connection providers", e );
		}

		return delegates;
	}
}
