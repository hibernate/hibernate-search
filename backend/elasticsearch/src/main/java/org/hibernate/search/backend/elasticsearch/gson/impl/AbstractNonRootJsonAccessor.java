/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;

abstract class AbstractNonRootJsonAccessor<P extends JsonElement, T> implements JsonAccessor<T> {
	private final JsonAccessor<P> parentAccessor;

	public AbstractNonRootJsonAccessor(JsonAccessor<P> parentAccessor) {
		super();
		this.parentAccessor = parentAccessor;
	}

	protected JsonAccessor<P> getParentAccessor() {
		return parentAccessor;
	}

	@Override
	public String toString() {
		StringBuilder path = new StringBuilder();

		if ( parentAccessor != RootJsonAccessor.INSTANCE ) {
			path.append( parentAccessor.toString() );
		}

		appendRuntimeRelativePath( path );

		return path.toString();
	}

	protected abstract void appendRuntimeRelativePath(StringBuilder path);

	protected abstract void appendStaticRelativePath(StringBuilder path, boolean first);
}
