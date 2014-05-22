/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Initialize object using one or several criteria queries.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CriteriaObjectInitializer implements ObjectInitializer {

	private static final Log log = LoggerFactory.make();
	private static final int MAX_IN_CLAUSE = 500;

	public static final CriteriaObjectInitializer INSTANCE = new CriteriaObjectInitializer();

	private CriteriaObjectInitializer() {
		// use INSTANCE instead of constructor
	}

	@Override
	public void initializeObjects(EntityInfo[] entityInfos,
								  Criteria criteria,
								  Class<?> entityType,
								  SearchFactoryImplementor searchFactoryImplementor,
								  TimeoutManager timeoutManager,
								  Session session) {
		//Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( log.isTraceEnabled() ) {
			log.tracef( "Load %d objects using criteria queries", maxResults );
		}

		if ( maxResults == 0 ) {
			return;
		}

		//criteria query not overridden, define one
		if ( criteria == null ) {
			criteria = session.createCriteria( entityType );
		}

		Set<Class<?>> indexedEntities = searchFactoryImplementor.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );
		DocumentBuilderIndexedEntity<?> builder = searchFactoryImplementor.getIndexBinding(
				indexedEntities.iterator().next()
		).getDocumentBuilder();
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
				ids.add( entityInfos[entityInfoIndex].getId() );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}
		criteria.add( disjunction );
		//not best effort so fail fast
		if ( timeoutManager.getType() != TimeoutManager.Type.LIMIT ) {
			Long timeLeftInSecond = timeoutManager.getTimeoutLeftInSeconds();
			if ( timeLeftInSecond != null ) {
				if ( timeLeftInSecond == 0 ) {
					timeoutManager.reactOnQueryTimeoutExceptionWhileExtracting( null );
				}
				criteria.setTimeout( timeLeftInSecond.intValue() );
			}
		}
		criteria.list(); //load all objects
	}
}
