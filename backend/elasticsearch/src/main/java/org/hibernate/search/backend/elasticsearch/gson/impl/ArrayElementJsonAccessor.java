/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

class ArrayElementJsonAccessor extends AbstractCrawlingJsonAccessor<JsonArray> {
	private final int index;

	public ArrayElementJsonAccessor(JsonCompositeAccessor<JsonArray> parentAccessor, int index) {
		super( parentAccessor );
		this.index = index;
	}

	@Override
	protected JsonElement doGet(JsonArray parent) {
		if ( parent != null && index < parent.size() ) {
			return parent.get( index );
		}
		else {
			return null;
		}
	}

	@Override
	protected void doSet(JsonArray parent, JsonElement newValue) {
		fillTo( parent, index );
		parent.set( index, newValue );
	}

	private static void fillTo(JsonArray array, int index) {
		for ( int i = array.size(); i <= index; ++i ) {
			array.add( JsonNull.INSTANCE );
		}
	}

	@Override
	protected void appendRuntimeRelativePath(StringBuilder path) {
		path.append( "[" ).append( index ).append( "]" );
	}

	@Override
	protected void appendStaticRelativePath(StringBuilder path, boolean first) {
		// Nothing to do
	}
}
