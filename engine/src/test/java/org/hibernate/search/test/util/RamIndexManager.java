/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.util;

import java.util.Properties;

import org.apache.lucene.search.DefaultSimilarity;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.impl.SimpleInitializer;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * At this point mainly used for tests
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RamIndexManager extends DirectoryBasedIndexManager {

	private static final LogErrorHandler logErrorHandler = new LogErrorHandler();

	public static RamIndexManager makeRamDirectory() {
		RamIndexManager ramIndexManager = new RamIndexManager();
		Properties properties = new Properties();
		properties.setProperty( "directory_provider", "ram" );
		ramIndexManager.initialize( "testIndex", properties, new DefaultSimilarity(), new EmptyWorkerBuildContext() );
		return ramIndexManager;
	}

	private static class EmptyWorkerBuildContext implements WorkerBuildContext {

		@Override
		public SearchFactoryImplementor getUninitializedSearchFactory() {
			return null;
		}

		@Override
		public String getIndexingStrategy() {
			return null;
		}

		@Override
		@Deprecated
		public <T> T requestService(Class<? extends ServiceProvider<T>> provider) {
			return null;
		}

		@Override
		@Deprecated
		public void releaseService(Class<? extends ServiceProvider<?>> provider) {
		}

		@Override
		public IndexManagerHolder getAllIndexesManager() {
			return null;
		}

		@Override
		public ErrorHandler getErrorHandler() {
			return logErrorHandler;
		}

		@Override
		public boolean isTransactionManagerExpected() {
			return false;
		}

		@Override
		public InstanceInitializer getInstanceInitializer() {
			return SimpleInitializer.INSTANCE;
		}

		@Override
		public boolean isIndexMetadataComplete() {
			return true;
		}

		@Override
		public ServiceManager getServiceManager() {
			return new ServiceManager() {
				@Override
				public <T> T requestService(Class<? extends ServiceProvider<T>> serviceProviderClass,
						BuildContext context) {
					return null;
				}
				@Override
				public void releaseService(Class<? extends ServiceProvider<?>> serviceProviderClass) {
				}
				@Override
				public void stopServices() {
				}
			};
		}
	}

}
