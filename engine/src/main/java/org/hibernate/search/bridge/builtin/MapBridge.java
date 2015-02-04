/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.util.Map;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.ContainerBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Each entry ({@code null included}) of the values in a {@link java.util.Map} is indexed using the specified {@link org.hibernate.search.bridge.FieldBridge}.
 * <br>
 * A {@code null} array is not indexed.
 *
 * @author Davide D'Alto
 */
public class MapBridge implements FieldBridge, ContainerBridge {

	private final FieldBridge bridge;

	/**
	 * @param bridge  the {@link org.hibernate.search.bridge.FieldBridge} used for each entry of a {@link java.util.Map} object.
	 */
	public MapBridge(FieldBridge bridge) {
		this.bridge = bridge;
	}


	@Override
	public void set(String fieldName, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			indexNotNullMap( fieldName, value, document, luceneOptions );
		}
	}

	@Override
	public FieldBridge getElementBridge() {
		return bridge;
	}

	private void indexNotNullMap(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Iterable<?> collection = ((Map<?,?>) value).values();
		for ( Object entry : collection ) {
			indexEntry( name, entry, document, luceneOptions );
		}
	}

	private void indexEntry(String fieldName, Object entry, Document document, LuceneOptions luceneOptions) {
		bridge.set( fieldName, entry, document, luceneOptions );
	}
}
