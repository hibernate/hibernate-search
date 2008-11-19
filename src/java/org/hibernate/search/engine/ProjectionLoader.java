//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.transform.ResultTransformer;

/**
 * Implementation of the <code>Loader</code> interface used for loading entities which are projected via
 * {@link org.hibernate.search.ProjectionConstants#THIS}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ProjectionLoader implements Loader {
	private SearchFactoryImplementor searchFactoryImplementor;
	private Session session;
	private Loader objectLoader;
	private Boolean projectThis;
	private ResultTransformer transformer;
	private String[] aliases;
	private Set<Class<?>> entityTypes;

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		this.session = session;
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor, ResultTransformer transformer, String[] aliases) {
		init( session, searchFactoryImplementor );
		this.transformer = transformer;
		this.aliases = aliases;
	}

	public void setEntityTypes(Set<Class<?>> entityTypes) {
		this.entityTypes = entityTypes;
	}

	public Object load(EntityInfo entityInfo) {
		initThisProjectionFlag( entityInfo );
		if ( projectThis ) {
			for ( int index : entityInfo.indexesOfThis ) {
				entityInfo.projection[index] = objectLoader.load( entityInfo );
			}
		}
		if ( transformer != null ) {
			return transformer.transformTuple( entityInfo.projection, aliases );
		}
		else {
			return entityInfo.projection;
		}
	}

	private void initThisProjectionFlag(EntityInfo entityInfo) {
		if ( projectThis == null ) {
			projectThis = entityInfo.indexesOfThis.size() != 0;
			if ( projectThis ) {
				MultiClassesQueryLoader loader = new MultiClassesQueryLoader();
				loader.init( session, searchFactoryImplementor );
				loader.setEntityTypes( entityTypes );
				objectLoader = loader;
			}
		}
	}

	public List load(EntityInfo... entityInfos) {
		List results = new ArrayList( entityInfos.length );
		if ( entityInfos.length == 0 ) {
			return results;
		}

		initThisProjectionFlag( entityInfos[0] );
		if ( projectThis ) {
			objectLoader.load( entityInfos ); // load by batch
			for ( EntityInfo entityInfo : entityInfos ) {
				for ( int index : entityInfo.indexesOfThis ) {
					// set one by one to avoid loosing null objects (skipped in the objectLoader.load( EntityInfo[] ))
					entityInfo.projection[index] = objectLoader.load( entityInfo );
				}
			}
		}
		for ( EntityInfo entityInfo : entityInfos ) {
			if ( transformer != null ) {
				results.add( transformer.transformTuple( entityInfo.projection, aliases ) );
			}
			else {
				results.add( entityInfo.projection );
			}
		}

		return results;
	}
}
