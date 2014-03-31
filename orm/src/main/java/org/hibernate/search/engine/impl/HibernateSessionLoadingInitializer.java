/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.engine.impl;

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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
