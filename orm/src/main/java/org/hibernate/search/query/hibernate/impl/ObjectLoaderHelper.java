/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.hcore.util.impl.HibernateHelper;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ObjectLoaderHelper {

	private static final Log log = LoggerFactory.make();

	private ObjectLoaderHelper() {
		// not allowed
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

	private static Object executeLoad(EntityInfo entityInfo, Session session) {
		Object maybeProxy;
		if ( areDocIdAndEntityIdIdentical( entityInfo, session ) ) {
			// be sure to get an initialized object but save from ONFE and ENFE
			maybeProxy = session.load( entityInfo.getClazz(), entityInfo.getId() );
		}
		else {
			Criteria criteria = session.createCriteria( entityInfo.getClazz() );
			criteria.add( Restrictions.eq( entityInfo.getIdName(), entityInfo.getId() ) );
			try {
				maybeProxy = criteria.uniqueResult();
			}
			catch (HibernateException e) {
				// FIXME should not raise an exception but return something like null
				// FIXME this happens when the index is out of sync with the db)
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

	// TODO should we cache that result?
	public static boolean areDocIdAndEntityIdIdentical(EntityInfo entityInfo, Session session) {
		String hibernateIdentifierProperty = session.getSessionFactory()
				.getClassMetadata( entityInfo.getClazz() )
				.getIdentifierPropertyName();
		return entityInfo.getIdName().equals( hibernateIdentifierProperty );
	}
}
