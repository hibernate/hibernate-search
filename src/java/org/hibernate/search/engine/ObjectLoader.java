//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoader implements Loader {
	private static final Logger log = LoggerFactory.getLogger( ObjectLoader.class );
	private Session session;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
	}

	public Object load(EntityInfo entityInfo) {
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

	public List load(EntityInfo... entityInfos) {
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
