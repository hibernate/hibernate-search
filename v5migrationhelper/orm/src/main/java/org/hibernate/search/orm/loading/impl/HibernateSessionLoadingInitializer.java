/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.orm.loading.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * This EntityInitializer is relative to a specific Hibernate Session,
 * so it's able to attach detached collections to it's Session.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class HibernateSessionLoadingInitializer extends HibernateStatelessInitializer implements InstanceInitializer {

	private final SessionImplementor hibernateSession;

	public HibernateSessionLoadingInitializer(SessionImplementor hibernateSession) {
		this.hibernateSession = hibernateSession;
	}

	@Override
	public Object unproxy(Object instance) {
		if ( instance instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) instance;
			final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
			Object initialized = lazyInitializer.getImplementation( hibernateSession );
			if ( initialized != null ) {
				return initialized;
			}
			else {
				// This is the case in which the proxy was created by a different session.
				// unproxyAndReassociate is the ultimate bomb,
				// able to deal with a Session change:
				return hibernateSession.getPersistenceContext().unproxyAndReassociate( proxy );
			}
		}
		return instance;
	}

	@Override
	public <T> Collection<T> initializeCollection(Collection<T> value) {
		//No action needed
		return value;
	}

	@Override
	public <K,V> Map<K,V> initializeMap(Map<K,V> value) {
		//No action needed
		return value;
	}

}
