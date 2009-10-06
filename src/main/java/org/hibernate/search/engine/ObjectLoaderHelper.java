/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoaderHelper {

	private static final int MAX_IN_CLAUSE = 500;
	private static final Logger log = LoggerFactory.make();

	public static Object load(EntityInfo entityInfo, Session session) {
		//be sure to get an initialized object but save from ONFE and ENFE
		Object maybeProxy = session.load( entityInfo.clazz, entityInfo.id );
		try {
			Hibernate.initialize( maybeProxy );
		}
		catch (RuntimeException e) {
			if ( LoaderHelper.isObjectNotFoundException( e ) ) {
				log.debug( "Object found in Search index but not in database: {} with id {}",
						entityInfo.clazz, entityInfo.id );
				maybeProxy = null;
			}
			else {
				throw e;
			}
		}
		return maybeProxy;
	}

	public static void initializeObjects(EntityInfo[] entityInfos, Criteria criteria, Class<?> entityType,
										 SearchFactoryImplementor searchFactoryImplementor) {
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) return;

		Set<Class<?>> indexedEntities = searchFactoryImplementor.getIndexedTypesPolymorphic( new Class<?>[]{entityType} );
		DocumentBuilderIndexedEntity<?> builder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( indexedEntities.iterator().next() );
		String idName = builder.getIdentifierName();
		int loop = maxResults / MAX_IN_CLAUSE;
		boolean exact = maxResults % MAX_IN_CLAUSE == 0;
		if ( !exact ) loop++;
		Disjunction disjunction = Restrictions.disjunction();
		for (int index = 0; index < loop; index++) {
			int max = index * MAX_IN_CLAUSE + MAX_IN_CLAUSE <= maxResults ?
					index * MAX_IN_CLAUSE + MAX_IN_CLAUSE :
					maxResults;
			List<Serializable> ids = new ArrayList<Serializable>( max - index * MAX_IN_CLAUSE );
			for (int entityInfoIndex = index * MAX_IN_CLAUSE; entityInfoIndex < max; entityInfoIndex++) {
				ids.add( entityInfos[entityInfoIndex].id );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}
		criteria.add( disjunction );
		criteria.list(); //load all objects
	}


	public static List returnAlreadyLoadedObjectsInCorrectOrder(EntityInfo[] entityInfos, Session session) {
		//mandatory to keep the same ordering
		List result = new ArrayList( entityInfos.length );
		for (EntityInfo entityInfo : entityInfos) {
			Object element = session.load( entityInfo.clazz, entityInfo.id );
			if ( Hibernate.isInitialized( element ) ) {
				//all existing elements should have been loaded by the query,
				//the other ones are missing ones
				result.add( element );
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debug( "Object found in Search index but not in database: {} with {}",
						entityInfo.clazz, entityInfo.id );
				}
			}
		}
		return result;
	}
}
