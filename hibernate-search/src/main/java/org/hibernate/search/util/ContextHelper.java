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
package org.hibernate.search.util;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.event.FullTextIndexEventListener;

/**
 * @author Emmanuel Bernard
 * @deprecated Use {@link org.hibernate.search.FullTextSession#getSearchFactory()} instead.
 */
public abstract class ContextHelper {

	public static SearchFactoryImplementor getSearchFactory(Session session) {
		return getSearchFactoryBySFI( (SessionImplementor) session );
	}

	public static SearchFactoryImplementor getSearchFactoryBySFI(SessionImplementor session) {
		PostInsertEventListener[] listeners = session.getListeners().getPostInsertEventListeners();
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
