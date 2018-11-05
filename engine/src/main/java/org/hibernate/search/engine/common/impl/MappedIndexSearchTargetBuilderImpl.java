/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetContextBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTargetBuilder;

class MappedIndexSearchTargetBuilderImpl implements MappedIndexSearchTargetBuilder {
	private final IndexSearchTargetContextBuilder delegate;

	MappedIndexSearchTargetBuilderImpl(IndexManagerImplementor<?> firstIndexManager,
			MappingContextImplementor mappingContext) {
		this.delegate = firstIndexManager.createSearchTargetContextBuilder( mappingContext );
	}

	void add(IndexManagerImplementor<?> indexManager) {
		indexManager.addToSearchTarget( delegate );
	}

	@Override
	public MappedIndexSearchTarget build() {
		return new MappedIndexSearchTargetImpl<>( delegate.build() );
	}
}
