/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lazily loaded services might need a {@code BuildContext} to retrieve additional services.
 * In such a case we can wrap a reference to a {@link ServiceManager} and a
 * {@link SearchFactoryImplementor} to create a limited BuildContext:
 * we're out of the boot phase at this point so not all operations are legal.
 *
 * This isn't great design but we need it to temporarily keep backwards compatibility,
 * so this class was deprecated since it's first version.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@Deprecated
public class EmptyBuildContext implements BuildContext {

	private static final Log log = LoggerFactory.make();

	private final ServiceManager services;
	private final SearchFactoryImplementor searchFactory;

	public EmptyBuildContext(ServiceManager delegate, SearchFactoryImplementor searchFactory) {
		this.services = delegate;
		this.searchFactory = searchFactory;
	}

	@Override
	@Deprecated
	public <T> T requestService(Class<? extends ServiceProvider<T>> provider) {
		return getServiceManager().requestService( provider, this );
	}

	@Override
	@Deprecated
	public void releaseService(Class<? extends ServiceProvider<?>> provider) {
		getServiceManager().releaseService( provider );
	}

	@Override
	public ServiceManager getServiceManager() {
		return services;
	}

	@Override
	public SearchFactoryImplementor getUninitializedSearchFactory() {
		return searchFactory;
	}

	/**
	 * To not be used on this BuildContext implementation.
	 * @throws org.hibernate.search.SearchException Always thrown.
	 */
	@Override
	public String getIndexingStrategy() {
		throw log.illegalServiceBuildPhase();
	}

	/**
	 * To not be used on this BuildContext implementation.
	 * @throws org.hibernate.search.SearchException Always thrown.
	 */
	@Override
	public IndexManagerHolder getAllIndexesManager() {
		throw log.illegalServiceBuildPhase();
	}

	/**
	 * To not be used on this BuildContext implementation.
	 * @throws org.hibernate.search.SearchException Always thrown.
	 */
	@Override
	public ErrorHandler getErrorHandler() {
		throw log.illegalServiceBuildPhase();
	}

}
