/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;

class PojoMassIndexerAgentCreateContextImpl implements PojoMassIndexerAgentCreateContext {
	private final PojoMassIndexingMappingContext mappingContext;
	private final String tenantIdentifier;

	PojoMassIndexerAgentCreateContextImpl(PojoMassIndexingMappingContext mappingContext, String tenantIdentifier) {
		this.mappingContext = mappingContext;
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public ScheduledExecutorService newScheduledExecutor(int threads, String threadNamePrefix) {
		return mappingContext.threadPoolProvider()
				.newScheduledExecutor( threads,
						PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + threadNamePrefix );
	}

	@Override
	public String tenantIdentifier() {
		return tenantIdentifier;
	}
}
