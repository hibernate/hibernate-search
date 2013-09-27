/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.impl.HibernateHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ObjectLoaderHelper {

	private static final Log log = LoggerFactory.make();

	private ObjectLoaderHelper() {
		//not allowed
	}

	public static Object load(EntityInfo entityInfo, Session session) {
		Object maybeProxy = executeLoad( entityInfo, session );
		try {
			HibernateHelper.initialize( maybeProxy );
		}
		catch (RuntimeException e) {
			if ( LoaderHelper.isObjectNotFoundException( e ) ) {
				log.debugf(
						"Object found in Search index but not in database: %s with id %s",
						entityInfo.getClazz(), entityInfo.getId()
				);
				session.evict( maybeProxy );
				maybeProxy = null;
			}
			else {
				throw e;
			}
		}
		return maybeProxy;
	}

	public static List returnAlreadyLoadedObjectsInCorrectOrder(EntityInfo[] entityInfos, Session session) {
		//mandatory to keep the same ordering
		List result = new ArrayList( entityInfos.length );
		for ( EntityInfo entityInfo : entityInfos ) {
			//FIXME This call is very inefficient when @Entity's id property is different
			//FIXME from Document stored id as we need to do the actual query again
			Object element = executeLoad( entityInfo, session );
			if ( element != null && HibernateHelper.isInitialized( element ) ) {
				//all existing elements should have been loaded by the query,
				//the other ones are missing ones
				result.add( element );
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Object found in Search index but not in database: %s with %s",
							entityInfo.getClazz(), entityInfo.getId()
					);
				}
			}
		}
		return result;
	}

	private static Object executeLoad(EntityInfo entityInfo, Session session) {
		Object maybeProxy;
		if ( areDocIdAndEntityIdIdentical( entityInfo, session ) ) {
			//be sure to get an initialized object but save from ONFE and ENFE
			maybeProxy = session.load( entityInfo.getClazz(), entityInfo.getId() );
		}
		else {
			Criteria criteria = session.createCriteria( entityInfo.getClazz() );
			criteria.add( Restrictions.eq( entityInfo.getIdName(), entityInfo.getId() ) );
			try {
				maybeProxy = criteria.uniqueResult();
			}
			catch (HibernateException e) {
				//FIXME should not raise an exception but return something like null
				//FIXME this happens when the index is out of sync with the db)
				throw new SearchException(
						"Loading entity of type " + entityInfo.getClazz().getName() + " using '"
								+ entityInfo.getIdName()
								+ "' as document id and '"
								+ entityInfo.getId()
								+ "' as value was not unique"
				);
			}
		}
		return maybeProxy;
	}

	//TODO should we cache that result?
	public static boolean areDocIdAndEntityIdIdentical(EntityInfo entityInfo, Session session) {
		String hibernateIdentifierProperty = session.getSessionFactory()
				.getClassMetadata( entityInfo.getClazz() )
				.getIdentifierPropertyName();
		return entityInfo.getIdName().equals( hibernateIdentifierProperty );
	}
}
