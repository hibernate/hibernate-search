/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldTypedContext<T> implements IndexSchemaFieldTypedContext<T> {

	private final String relativeName;
	private final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	private IndexFieldAccessor<T> accessor;

	StubIndexSchemaFieldTypedContext(String relativeName, StubIndexSchemaNode.Builder builder, boolean included) {
		this.relativeName = relativeName;
		this.builder = builder;
		this.included = included;
	}

	@Override
	public IndexSchemaFieldTypedContext<T> analyzer(String analyzerName) {
		builder.analyzerName( analyzerName );
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<T> normalizer(String normalizerName) {
		builder.normalizerName( normalizerName );
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<T> store(Store store) {
		builder.store( store );
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<T> sortable(Sortable sortable) {
		builder.sortable( sortable );
		return this;
	}

	@Override
	public IndexFieldAccessor<T> createAccessor() {
		if ( accessor == null ) {
			if ( included ) {
				accessor = new StubIncludedIndexFieldAccessor<>( relativeName );
			}
			else {
				accessor = new StubExcludedIndexFieldAccessor<>( relativeName );
			}
		}
		return accessor;
	}
}
