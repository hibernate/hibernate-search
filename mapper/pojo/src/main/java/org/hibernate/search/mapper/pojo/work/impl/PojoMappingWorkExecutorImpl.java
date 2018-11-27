/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.work.spi.PojoMappingWorkExecutor;

public class PojoMappingWorkExecutorImpl implements PojoMappingWorkExecutor {

	@Override
	public CompletableFuture<?> optimize(Collection<Class<?>> types) {
		// TODO must be implemented
		return CompletableFuture.completedFuture( "OK" );
	}

	@Override
	public CompletableFuture<?> purge(Collection<Class<?>> types) {
		// TODO must be implemented
		return CompletableFuture.completedFuture( "OK" );
	}

	@Override
	public CompletableFuture<?> flush(Collection<Class<?>> types) {
		// TODO must be implemented
		return CompletableFuture.completedFuture( "OK" );
	}

}
