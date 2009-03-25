//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoader implements Loader {
	private static final Logger log = LoggerFactory.make();
	private Session session;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
	}

	public Object load(EntityInfo entityInfo) {
		return ObjectLoaderHelper.load( entityInfo, session );
	}

	public List load(EntityInfo... entityInfos) {
		if ( entityInfos.length == 0 ) return Collections.EMPTY_LIST;
		if ( entityInfos.length == 1 ) {
			final Object entity = load( entityInfos[0] );
			if ( entity == null ) {
				return Collections.EMPTY_LIST;
			}
			else {
				final List<Object> list = new ArrayList<Object>( 1 );
				list.add( entity );
				return list;
			}
		}
		//use load to benefit from the batch-size
		//we don't face proxy casting issues since the exact class is extracted from the index
		for (EntityInfo entityInfo : entityInfos) {
			session.load( entityInfo.clazz, entityInfo.id );
		}
		List result = new ArrayList( entityInfos.length );
		for (EntityInfo entityInfo : entityInfos) {
			try {
				Object entity = session.load( entityInfo.clazz, entityInfo.id );
				Hibernate.initialize( entity );
				result.add( entity );
			}
			catch (RuntimeException e) {
				if ( LoaderHelper.isObjectNotFoundException( e ) ) {
					log.debug( "Object found in Search index but not in database: {} with id {}",
							entityInfo.clazz, entityInfo.id );
				}
				else {
					throw e;
				}
			}
		}
		return result;
	}
}
