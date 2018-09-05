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

/**
 * Each entry ({@code null included}) of an {@link java.lang.Iterable} object is indexed using the specified
 * {@link org.hibernate.search.bridge.FieldBridge}.
 * <br>
 * A {@code null} {@link java.lang.Iterable} object is not indexed.
 *
 * @author Davide D'Alto
 */
public class IterableBridge implements FieldBridge, ContainerBridge {

	private final FieldBridge bridge;

	/**
	 * @param bridge the {@link org.hibernate.search.bridge.FieldBridge} used for each entry of the {@link java.lang.Iterable} object.
	 */
	public IterableBridge(FieldBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public void set(String fieldName, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			indexNotNullIterable( fieldName, value, document, luceneOptions );
		}
	}

	@Override
	public FieldBridge getElementBridge() {
		return bridge;
	}

	private void indexNotNullIterable(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Iterable<?> collection = (Iterable<?>) value;
		for ( Object entry : collection ) {
			indexEntry( name, entry, document, luceneOptions );
		}
	}

	private void indexEntry(String fieldName, Object entry, Document document, LuceneOptions luceneOptions) {
		bridge.set( fieldName, entry, document, luceneOptions );
	}

}
