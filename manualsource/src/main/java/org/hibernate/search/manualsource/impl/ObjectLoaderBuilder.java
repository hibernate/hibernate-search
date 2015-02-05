/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.manualsource.source.ObjectInitializer;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoaderBuilder {
	private List<Class<?>> targetedEntities;
	private WorkLoadImpl workLoad;
	private ExtendedSearchIntegrator extendedIntegrator;
	private Set<Class<?>> indexedTargetedEntities;
	private TimeoutManager timeoutManager;
	private ObjectLookupMethod lookupMethod;
	private DatabaseRetrievalMethod retrievalMethod;
	private static final Log log = LoggerFactory.make();

	public ObjectLoaderBuilder targetedEntities(List<Class<?>> targetedEntities) {
		this.targetedEntities = targetedEntities;
		return this;
	}

	public ObjectLoaderBuilder lookupMethod(ObjectLookupMethod lookupMethod) {
		this.lookupMethod = lookupMethod;
		return this;
	}

	public ObjectLoaderBuilder retrievalMethod(DatabaseRetrievalMethod retrievalMethod) {
		this.retrievalMethod = retrievalMethod;
		return this;
	}

	public Loader buildLoader() {
		return getMultipleEntitiesLoader();
	}

	private Loader getMultipleEntitiesLoader() {
		final MultiClassesQueryLoader multiClassesLoader = new MultiClassesQueryLoader();
		multiClassesLoader.init( workLoad, extendedIntegrator, getObjectInitializer(), timeoutManager );
		multiClassesLoader.setEntityTypes( indexedTargetedEntities );
		return multiClassesLoader;
	}

	public ObjectLoaderBuilder workLoad(WorkLoadImpl workLoad) {
		this.workLoad = workLoad;
		return this;
	}

	public ObjectLoaderBuilder searchFactory(ExtendedSearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator;
		return this;
	}

	public ObjectLoaderBuilder indexedTargetedEntities(Set<Class<?>> indexedTargetedEntities) {
		this.indexedTargetedEntities = indexedTargetedEntities;
		return this;
	}

	public ObjectLoaderBuilder timeoutManager(TimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
		return this;
	}

	private ObjectInitializer getObjectInitializer() {
		return this.workLoad.getWorkLoadManager().getObjectInitializer();
	}
}
