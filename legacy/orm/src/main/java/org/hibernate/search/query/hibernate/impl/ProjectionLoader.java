/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.transform.ResultTransformer;

/**
 * Implementation of the {@code Loader} interface used for loading entities which are projected via
 * {@link org.hibernate.search.engine.ProjectionConstants#THIS}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ProjectionLoader implements Loader {
	private Loader objectLoader;
	private boolean projectThis;
	private ResultTransformer transformer;
	private String[] aliases;
	private ObjectLoaderBuilder loaderBuilder;

	@Override
	public void init(SessionImplementor session,
					ExtendedSearchIntegrator extendedIntegrator,
					ObjectInitializer objectInitializer,
					TimeoutManager timeoutManager) {
	}

	public void init(SessionImplementor session,
					ExtendedSearchIntegrator extendedIntegrator,
					ResultTransformer transformer,
					ObjectLoaderBuilder loaderBuilder,
					String[] aliases,
					TimeoutManager timeoutManager,
					boolean projectThis) {
		init( session, extendedIntegrator, null, timeoutManager ); // TODO why do we call this method?
		this.transformer = transformer;
		this.aliases = aliases;
		this.loaderBuilder = loaderBuilder;
		this.projectThis = projectThis;
	}

	@Override
	public Object load(EntityInfo entityInfo) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		if ( projectThis ) {
			Loader objectLoader = getObjectLoader();
			final Object entityInstance = objectLoader.load( entityInfo );
			entityInfo.populateWithEntityInstance( entityInstance );
		}
		if ( transformer != null ) {
			return transformer.transformTuple( entityInfo.getProjection(), aliases );
		}
		else {
			return entityInfo.getProjection();
		}
	}

	@Override
	public Object loadWithoutTiming(EntityInfo entityInfo) {
		throw new AssertionFailure( "This method is not meant to be used on ProjectionLoader" );
	}

	@Override
	public List load(List<EntityInfo> entityInfos) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		List results = new ArrayList( entityInfos.size() );
		if ( entityInfos.isEmpty() ) {
			return results;
		}

		if ( projectThis ) {
			Loader objectLoader = getObjectLoader();
			objectLoader.load( entityInfos ); // load by batch
			for ( EntityInfo entityInfo : entityInfos ) {
				final Object entityInstance = objectLoader.loadWithoutTiming( entityInfo );
				entityInfo.populateWithEntityInstance( entityInstance );
			}
		}
		for ( EntityInfo entityInfo : entityInfos ) {
			if ( transformer != null ) {
				results.add( transformer.transformTuple( entityInfo.getProjection(), aliases ) );
			}
			else {
				results.add( entityInfo.getProjection() );
			}
		}

		if ( transformer != null ) {
			return transformer.transformList( results );
		}

		return results;
	}

	private Loader getObjectLoader() {
		if ( objectLoader == null ) {
			objectLoader = loaderBuilder.buildLoader();
		}
		return objectLoader;
	}

	@Override
	public boolean isSizeSafe() {
		return getObjectLoader().isSizeSafe();
	}
}
