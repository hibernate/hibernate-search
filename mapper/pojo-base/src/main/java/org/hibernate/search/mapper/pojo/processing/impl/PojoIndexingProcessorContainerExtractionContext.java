/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;

final class PojoIndexingProcessorContainerExtractionContext implements ContainerExtractionContext {

	static final PojoIndexingProcessorContainerExtractionContext INSTANCE =
			new PojoIndexingProcessorContainerExtractionContext();

	private PojoIndexingProcessorContainerExtractionContext() {
	}

	@Override
	public void propagateOrIgnoreContainerExtractionException(RuntimeException exception) {
		// We always propagate exceptions during indexing.
		throw exception;
	}
}
