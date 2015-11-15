/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.manualsource.source.ObjectInitializer;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Implementation of the {@code Loader} interface used for loading entities which are projected via
 * {@link org.hibernate.search.engine.ProjectionConstants#THIS}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ProjectionLoader implements Loader {
	private Loader objectLoader;
	private boolean projectThisIsInitialized = false;//guard for next variable
	private boolean projectThis;
	private String[] aliases;
	private ObjectLoaderBuilder loaderBuilder;

	@Override
	public void init(WorkLoadImpl workLoad,
					ExtendedSearchIntegrator extendedSearchIntegrator,
					ObjectInitializer objectInitializer,
					TimeoutManager timeoutManager) {
	}

	public void init(WorkLoadImpl workLoad,
					ExtendedSearchIntegrator extendedSearchIntegrator,
					ObjectLoaderBuilder loaderBuilder,
					String[] aliases,
					TimeoutManager timeoutManager) {
		init( workLoad, extendedSearchIntegrator, null, timeoutManager ); // TODO why do we call this method?
		this.aliases = aliases;
		this.loaderBuilder = loaderBuilder;
	}

	@Override
	public Object load(EntityInfo entityInfo) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		if ( projectionEnabledOnThis( entityInfo ) ) {
			Loader objectLoader = getObjectLoader();
			final Object entityInstance = objectLoader.load( entityInfo );
			entityInfo.populateWithEntityInstance( entityInstance );
		}
		return entityInfo.getProjection();
	}

	@Override
	public Object loadWithoutTiming(EntityInfo entityInfo) {
		throw new AssertionFailure( "This method is not meant to be used on ProjectionLoader" );
	}

	private boolean projectionEnabledOnThis(final EntityInfo entityInfo) {
		if ( projectThisIsInitialized == false ) {
			projectThisIsInitialized = true;
			projectThis = entityInfo.isProjectThis();
		}
		return projectThis;
	}

	@Override
	public List load(EntityInfo... entityInfos) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		List results = new ArrayList( entityInfos.length );
		if ( entityInfos.length == 0 ) {
			return results;
		}

		if ( projectionEnabledOnThis( entityInfos[0] ) ) {
			Loader objectLoader = getObjectLoader();
			objectLoader.load( entityInfos ); // load by batch
			for ( EntityInfo entityInfo : entityInfos ) {
				final Object entityInstance = objectLoader.loadWithoutTiming( entityInfo );
				entityInfo.populateWithEntityInstance( entityInstance );
			}
		}
		for ( EntityInfo entityInfo : entityInfos ) {
			results.add( entityInfo.getProjection() );
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
