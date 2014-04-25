/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.testsupport.setup;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;

/**
 * A {@code BuildContext} implementation for running tests.
 *
 * @author Hardy Ferentschik
 */
public class BuildContextForTest implements BuildContext {
	private static final String INDEXING_STRATEGY_EVENT = "event";
	private final SearchConfiguration searchConfiguration;

	public BuildContextForTest(SearchConfiguration searchConfiguration) {
		this.searchConfiguration = searchConfiguration;
	}

	@Override
	public SearchFactoryImplementor getUninitializedSearchFactory() {
		return null;
	}

	@Override
	public String getIndexingStrategy() {
		return INDEXING_STRATEGY_EVENT;
	}

	@Override
	public ServiceManager getServiceManager() {
		return new StandardServiceManager( searchConfiguration, this, Environment.DEFAULT_SERVICES_MAP );
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return new IndexManagerHolder();
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return new LogErrorHandler();
	}
}


