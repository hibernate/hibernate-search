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
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.ObjectLoaderBuilder;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.query.impl.ObjectsInitializer;
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
	private Boolean projectThis;
	private ResultTransformer transformer;
	private String[] aliases;
	private ObjectLoaderBuilder loaderBuilder;

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
		init(session, searchFactoryImplementor, null, timeoutManager); //TODO why do we call this method?
		this.transformer = transformer;
		this.aliases = aliases;
		this.loaderBuilder = loaderBuilder;
	}

	public Object load(EntityInfo entityInfo) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		initThisProjectionFlag(entityInfo);
		if (projectThis) {
			for (int index : entityInfo.indexesOfThis) {
				entityInfo.projection[index] = objectLoader.load(entityInfo);
			}
		}
		if (transformer != null) {
			return transformer.transformTuple(entityInfo.projection, aliases);
		}
		else {
			return entityInfo.projection;
		}
	}

	public Object loadWithoutTiming(EntityInfo entityInfo) {
		throw new AssertionFailure("This method is not meant to be used on ProjectionLoader");
	}

	private void initThisProjectionFlag(EntityInfo entityInfo) {
		if (projectThis == null) {
			projectThis = entityInfo.indexesOfThis.size() != 0;
			if (projectThis) {
				objectLoader = loaderBuilder.buildLoader();
			}
		}
	}

	public List load(EntityInfo... entityInfos) {
		//no need to timeouManage here, the underlying loader is the real time consumer
		List results = new ArrayList(entityInfos.length);
		if (entityInfos.length == 0) {
			return results;
		}

		initThisProjectionFlag(entityInfos[0]);
		if (projectThis) {
			objectLoader.load(entityInfos); // load by batch
			for (EntityInfo entityInfo : entityInfos) {
				for (int index : entityInfo.indexesOfThis) {
					// set one by one to avoid loosing null objects (skipped in the objectLoader.load( EntityInfo[] ))
					// use objectLoader.executeLoad to prevent measuring load time again (see AbstractLoader)
					entityInfo.projection[index] = objectLoader.loadWithoutTiming(entityInfo);
				}
			}
		}
		for (EntityInfo entityInfo : entityInfos) {
			if (transformer != null) {
				results.add(transformer.transformTuple(entityInfo.projection, aliases));
			}
			else {
				results.add(entityInfo.projection);
			}
		}

		return results;
	}
}
