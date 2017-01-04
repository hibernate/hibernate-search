/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.ContainerBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * Each entry ({@code null included}) of an array is indexed using the specified {@link org.hibernate.search.bridge.FieldBridge}.
 * <br>
 * A {@code null} array is not indexed.
 *
 * @author Davide D'Alto
 */
public class ArrayBridge implements FieldBridge, ContainerBridge {

	private final FieldBridge bridge;

	/**
	 * @param bridge the {@link org.hibernate.search.bridge.FieldBridge} used for each entry of the array
	 */
	public ArrayBridge(FieldBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			indexNotNullArray( name, value, document, luceneOptions );
		}
	}

	@Override
	public FieldBridge getElementBridge() {
		return bridge;
	}

	private void indexNotNullArray(String name, Object value, Document document, LuceneOptions luceneOptions) {
		// Use CollectionHelper.iterableFromArray to also support arrays of primitive values
		for ( Object entry : CollectionHelper.iterableFromArray( value ) ) {
			indexEntry( name, entry, document, luceneOptions );
		}
	}

	private void indexEntry(String fieldName, Object entry, Document document, LuceneOptions luceneOptions) {
		bridge.set( fieldName, entry, document, luceneOptions );
	}
}
