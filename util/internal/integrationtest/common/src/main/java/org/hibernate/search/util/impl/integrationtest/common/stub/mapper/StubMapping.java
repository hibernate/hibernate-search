/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;

public class StubMapping implements MappingImplementor<StubMapping> {

	private final Map<String, MappedIndexManager<?>> indexManagersByTypeIdentifier;

	StubMapping(Map<String, MappedIndexManager<?>> indexManagersByTypeIdentifier) {
		this.indexManagersByTypeIdentifier = indexManagersByTypeIdentifier;
	}

	@Override
	public StubMapping toAPI() {
		return this;
	}

	public MappedIndexManager<?> getIndexManagerByTypeIdentifier(String typeId) {
		return indexManagersByTypeIdentifier.get( typeId );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
