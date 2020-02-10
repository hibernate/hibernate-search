/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;

public class StubMappingPartialBuildState implements MappingPartialBuildState {

	private final Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier;

	StubMappingPartialBuildState(Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier) {
		this.indexMappingsByTypeIdentifier = indexMappingsByTypeIdentifier;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	public MappingImplementor<StubMapping> finalizeMapping() {
		return new StubMapping( indexMappingsByTypeIdentifier );
	}

}
