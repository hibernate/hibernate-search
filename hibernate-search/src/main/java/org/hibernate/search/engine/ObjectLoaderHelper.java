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
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.SearchException;
import org.hibernate.search.query.TimeoutManager;
import org.hibernate.search.util.HibernateHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ObjectLoaderHelper {

	private static final int MAX_IN_CLAUSE = 500;
	private static final Logger log = LoggerFactory.make();

	public static Object load(EntityInfo entityInfo, Session session) {
		Object maybeProxy = executeLoad( entityInfo, session );
		try {
			HibernateHelper.initialize( maybeProxy );
		}
		catch ( RuntimeException e ) {
			if ( LoaderHelper.isObjectNotFoundException( e ) ) {
				log.debug(
						"Object found in Search index but not in database: {} with id {}",
						entityInfo.clazz, entityInfo.id
				);
				maybeProxy = null;
			}
			else {
				throw e;
			}
		}
		return maybeProxy;
	}

	public static void initializeObjects(EntityInfo[] entityInfos,
										 Criteria criteria, Class<?> entityType,
										 SearchFactoryImplementor searchFactoryImplementor,
										 TimeoutManager timeoutManager) {
		//Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) {
			return;
		}

		Set<Class<?>> indexedEntities = searchFactoryImplementor.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );
		DocumentBuilderIndexedEntity<?> builder = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				indexedEntities.iterator().next()
		);
		String idName = builder.getIdentifierName();
		Disjunction disjunction = Restrictions.disjunction();

		int loop = maxResults / MAX_IN_CLAUSE;
		boolean exact = maxResults % MAX_IN_CLAUSE == 0;
		if ( !exact ) {
			loop++;
		}
		for ( int index = 0; index < loop; index++ ) {
			int max = index * MAX_IN_CLAUSE + MAX_IN_CLAUSE <= maxResults ?
					index * MAX_IN_CLAUSE + MAX_IN_CLAUSE :
					maxResults;
			List<Serializable> ids = new ArrayList<Serializable>( max - index * MAX_IN_CLAUSE );
			for ( int entityInfoIndex = index * MAX_IN_CLAUSE; entityInfoIndex < max; entityInfoIndex++ ) {
				ids.add( entityInfos[entityInfoIndex].id );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}
		criteria.add( disjunction );
		//not best effort so fail fast
		if ( timeoutManager.getType() != TimeoutManager.Type.LIMIT ) {
			Long timeLeftInSecond = timeoutManager.getTimeoutLeftInSeconds();
			if ( timeLeftInSecond != null ) {
				if (timeLeftInSecond == 0) {
					timeoutManager.reactOnQueryTimeoutExceptionWhileExtracting(null);
				}
				criteria.setTimeout( timeLeftInSecond.intValue() );
			}
		}
		criteria.list(); //load all objects
	}


	public static List returnAlreadyLoadedObjectsInCorrectOrder(EntityInfo[] entityInfos, Session session) {
		//mandatory to keep the same ordering
		List result = new ArrayList( entityInfos.length );
		for ( EntityInfo entityInfo : entityInfos ) {
			//FIXME This call is very inefficient when @Entity's id property is different
			//FIXME from Document stored id as we need to do the actual query again
			Object element = executeLoad( entityInfo, session );
			if ( HibernateHelper.isInitialized( element ) ) {
				//all existing elements should have been loaded by the query,
				//the other ones are missing ones
				result.add( element );
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debug(
							"Object found in Search index but not in database: {} with {}",
							entityInfo.clazz, entityInfo.id
					);
				}
			}
		}
		return result;
	}

	private static Object executeLoad(EntityInfo entityInfo, Session session) {
		Object maybeProxy;
		String hibernateIdentifierProperty = session.getSessionFactory()
				.getClassMetadata( entityInfo.clazz )
				.getIdentifierPropertyName();

		if ( entityInfo.idName.equals( hibernateIdentifierProperty ) ) {
			//be sure to get an initialized object but save from ONFE and ENFE
			maybeProxy = session.load( entityInfo.clazz, entityInfo.id );
		}
		else {
			Criteria criteria = session.createCriteria( entityInfo.clazz );
			criteria.add( Restrictions.eq( entityInfo.idName, entityInfo.id ) );
			try {
				maybeProxy = criteria.uniqueResult();
			}
			catch ( HibernateException e ) {
				//FIXME should not raise an exception but return something like null
				//FIXME this happens when the index is out of sync with the db)
				throw new SearchException(
						"Loading entity of type " + entityInfo.clazz.getName() + " using '"
								+ entityInfo.idName
								+ "' as document id and '"
								+ entityInfo.id
								+ "'  as value was not unique"
				);
			}
		}
		return maybeProxy;
	}
}
