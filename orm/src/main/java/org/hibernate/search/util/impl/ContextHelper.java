/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.util.impl;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.event.impl.FullTextIndexEventListener;

/**
 * @author Emmanuel Bernard
 * @deprecated Use {@link org.hibernate.search.FullTextSession#getSearchFactory()} instead.
 */
public class ContextHelper {

	private ContextHelper() {
	}

	public static SearchFactoryImplementor getSearchFactory(Session session) {
		return getSearchFactoryBySessionImplementor( (SessionImplementor) session );
	}

	public static SearchFactoryImplementor getSearchFactoryBySessionImplementor(SessionImplementor session) {
		return getSearchFactoryBySFI( session.getFactory() );
	}

	public static SearchFactoryImplementor getSearchFactoryBySFI(SessionFactoryImplementor sfi) {
		final EventListenerRegistry service = sfi
				.getServiceRegistry()
				.getService( EventListenerRegistry.class );
		final Iterable<PostInsertEventListener> listeners = service.getEventListenerGroup( EventType.POST_INSERT )
				.listeners();
		FullTextIndexEventListener listener = null;
		//FIXME this sucks since we mandate the event listener use
		for ( PostInsertEventListener candidate : listeners ) {
			if ( candidate instanceof FullTextIndexEventListener ) {
				listener = (FullTextIndexEventListener) candidate;
				break;
			}
		}
		if ( listener == null ) {
			throw new HibernateException(
					"Hibernate Search Event listeners not configured, please check the reference documentation and the " +
							"application's hibernate.cfg.xml"
			);
		}
		return listener.getSearchFactoryImplementor();
	}
}
