/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;

public class StubMapping implements MappingImplementor<StubMapping> {

	private final Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier;

	StubMapping(Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier) {
		this.indexMappingsByTypeIdentifier = indexMappingsByTypeIdentifier;
	}

	@Override
	public StubMapping toConcreteType() {
		return this;
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop(MappingPreStopContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	public StubMappingIndexManager getIndexMappingByTypeIdentifier(String typeId) {
		return indexMappingsByTypeIdentifier.get( typeId );
	}
}
