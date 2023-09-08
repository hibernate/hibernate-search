/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.multitenancy.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.env.ConnectionProviderBuilder;

import org.jboss.logging.Logger;

public class H2LazyMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider
		implements Stoppable {

	private static final Logger log = Logger.getLogger( H2LazyMultiTenantConnectionProvider.class.getName() );

	private final String[] tenantIds;
	private final Map<String, ConnectionProvider> delegates = new HashMap<>();

	public H2LazyMultiTenantConnectionProvider(String[] tenantIds) {
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
	public ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		ConnectionProvider connectionProviderImpl = getOrCreateDelegates().get( tenantIdentifier );
		if ( connectionProviderImpl == null ) {
			throw new HibernateException( "Unknown tenant identifier" );
		}
		return connectionProviderImpl;
	}

	private Map<String, ConnectionProvider> getOrCreateDelegates() {
		if ( !delegates.isEmpty() ) {
			return delegates;
		}
		for ( String tenantId : tenantIds ) {
			ConnectionProvider connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( tenantId );
			delegates.put( tenantId, connectionProvider );
		}
		return delegates;
	}
}
