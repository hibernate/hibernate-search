/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.transform.ResultTransformer;

/**
 * Implementation of the {@code Loader} interface used for loading entities which are projected via
 * {@link org.hibernate.search.ProjectionConstants#THIS}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ProjectionLoader implements Loader {
	private Loader objectLoader;
	private boolean projectThisIsInitialized = false;//guard for next variable
	private boolean projectThis;
	private ResultTransformer transformer;
	private String[] aliases;
	private ObjectLoaderBuilder loaderBuilder;

	@Override
	public void init(Session session,
					SearchFactoryImplementor searchFactoryImplementor,
					ObjectsInitializer objectsInitializer,
					TimeoutManager timeoutManager) {
	}

	public void init(Session session,
					SearchFactoryImplementor searchFactoryImplementor,
					ResultTransformer transformer,
					ObjectLoaderBuilder loaderBuilder,
					String[] aliases,
					TimeoutManager timeoutManager) {
		init( session, searchFactoryImplementor, null, timeoutManager ); // TODO why do we call this method?
		this.transformer = transformer;
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
			if ( transformer != null ) {
				results.add( transformer.transformTuple( entityInfo.getProjection(), aliases ) );
			}
			else {
				results.add( entityInfo.getProjection() );
			}
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
