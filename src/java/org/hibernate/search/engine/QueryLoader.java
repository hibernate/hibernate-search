//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class QueryLoader implements Loader {
	private static final int MAX_IN_CLAUSE = 500;
	private final Logger log = LoggerFactory.getLogger( QueryLoader.class );

	private Session session;
	private Class entityType;
	private SearchFactoryImplementor searchFactoryImplementor;
	private Criteria criteria;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public void setEntityType(Class entityType) {
		this.entityType = entityType;
	}

	public Object load(EntityInfo entityInfo) {
		//be sure to get an initialized object
		Object maybeProxy = session.get( entityInfo.clazz, entityInfo.id );
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

	public List load(EntityInfo... entityInfos) {
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) return Collections.EMPTY_LIST;
		if ( entityType == null ) throw new AssertionFailure( "EntityType not defined" );
		if ( criteria == null ) criteria = session.createCriteria( entityType );

		DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( entityType );
		String idName = builder.getIdentifierName();
		int loop = maxResults / MAX_IN_CLAUSE;
		boolean exact = maxResults % MAX_IN_CLAUSE == 0;
		if ( !exact ) loop++;
		Disjunction disjunction = Restrictions.disjunction();
		for (int index = 0; index < loop; index++) {
			int max = index * MAX_IN_CLAUSE + MAX_IN_CLAUSE <= maxResults ?
					index * MAX_IN_CLAUSE + MAX_IN_CLAUSE :
					maxResults;
			List ids = new ArrayList( max - index * MAX_IN_CLAUSE );
			for (int entityInfoIndex = index * MAX_IN_CLAUSE; entityInfoIndex < max; entityInfoIndex++) {
				ids.add( entityInfos[entityInfoIndex].id );
			}
			disjunction.add( Restrictions.in( idName, ids ) );
		}
		criteria.add( disjunction );
		criteria.list(); //load all objects

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

	public void setCriteria(Criteria criteria) {
		this.criteria = criteria;
	}
}
