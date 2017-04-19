/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Properties;

import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;


/**
 * @author Yoann Rodiere
 */
public class ReflectionBasedBackend implements Backend {

	private final Class<? extends BackendQueueProcessor> backendQueueProcessorClass;

	private Properties properties;

	public ReflectionBasedBackend(Class<? extends BackendQueueProcessor> backendQueueProcessorClass) {
		this.backendQueueProcessorClass = backendQueueProcessorClass;
	}

	@Override
	public void initialize(Properties properties, WorkerBuildContext context) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( backendQueueProcessorClass )
				.append( "]" )
				.toString();
	}

	@Override
	public boolean isTransactional() {
		return BackendQueueProcessor.Transactional.class.isAssignableFrom( backendQueueProcessorClass );
	}

	@Override
	@SuppressWarnings("deprecation")
	public BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		BackendQueueProcessor backendQueueProcessor = ClassLoaderHelper.instanceFromClass(
				BackendQueueProcessor.class,
				backendQueueProcessorClass,
				"Backend queue processor " );
		backendQueueProcessor.initialize( properties, context, indexManager );
		return backendQueueProcessor;
	}

}
