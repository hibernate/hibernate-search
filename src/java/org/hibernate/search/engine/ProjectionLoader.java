//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class ProjectionLoader implements Loader {
	private SearchFactoryImplementor searchFactoryImplementor;
	private Session session;
	private ObjectLoader objectLoader;
	private Boolean projectThis;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public Object load(EntityInfo entityInfo) {
		initThisProjectionFlag( entityInfo );
		if ( projectThis ) {
			for (int index : entityInfo.indexesOfThis) {
				entityInfo.projection[index] = objectLoader.load( entityInfo );
			}
		}
		return entityInfo.projection;
	}

	private void initThisProjectionFlag(EntityInfo entityInfo) {
		if ( projectThis == null ) {
			projectThis = entityInfo.indexesOfThis != null;
			if ( projectThis ) {
				//TODO use QueryLoader when possible
				objectLoader = new ObjectLoader();
				objectLoader.init( session, searchFactoryImplementor );
			}
		}
	}

	public List load(EntityInfo... entityInfos) {
		List results = new ArrayList( entityInfos.length );
		if ( entityInfos.length == 0 ) return results;

		initThisProjectionFlag( entityInfos[0] );
		if ( projectThis ) {
			objectLoader.load( entityInfos ); //load by batch
			for (EntityInfo entityInfo : entityInfos) {
				for (int index : entityInfo.indexesOfThis) {
					//set one by one to avoid loosing null objects (skipped in the objectLoader.load( EntityInfo[] ))
					entityInfo.projection[index] = objectLoader.load( entityInfo );
				}
			}
		}
		for (EntityInfo entityInfo : entityInfos) {
			results.add( entityInfo.projection );
		}

		return results;
	}
}
