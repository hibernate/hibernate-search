//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.search.engine.EntityInfo;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoader implements Loader {
	private static final Log log = LogFactory.getLog( ObjectLoader.class );
	private Session session;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
	}

	public Object load(EntityInfo entityInfo) {
		//be sure to get an initialized object
		Object maybeProxy = session.get( entityInfo.clazz, entityInfo.id );
		try {
			Hibernate.initialize( maybeProxy );
		}
		catch (ObjectNotFoundException e) {
			log.debug( "Object found in Search index but not in database: "
					+ e.getEntityName() + " wih id " + e.getIdentifier() );
			maybeProxy = null;
		}
		return maybeProxy;
	}

	public List load(EntityInfo... entityInfos) {
		//use load to benefit from the batch-size
		//we don't face proxy casting issues since the exact class is extracted from the index
		for (EntityInfo entityInfo : entityInfos) {
			session.load( entityInfo.clazz, entityInfo.id );
		}
		List result = new ArrayList(entityInfos.length);
		for (EntityInfo entityInfo : entityInfos) {
			try {
				Object entity = session.load( entityInfo.clazz, entityInfo.id );
				Hibernate.initialize( entity );
				result.add( entity );
			}
			catch (ObjectNotFoundException e) {
				log.debug( "Object found in Search index but not in database: "
						+ e.getEntityName() + " wih id " + e.getIdentifier() );
			}
		}
		return result;
	}
}
