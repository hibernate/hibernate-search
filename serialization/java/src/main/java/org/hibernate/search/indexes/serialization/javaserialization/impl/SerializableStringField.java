/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.SerializableIndex;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializableStringField extends SerializableField {

	private String value;
	private SerializableStore store;
	private SerializableIndex index;
	private SerializableTermVector termVector;

	public SerializableStringField(LuceneFieldContext context) {
		super( context );
		this.value = context.getStringValue();
		this.store = context.getStore();
		this.index = context.getIndex();
		this.termVector = context.getTermVector();
	}

	public String getValue() {
		return value;
	}

	public SerializableStore getStore() {
		return store;
	}

	public SerializableIndex getIndex() {
		return index;
	}

	public SerializableTermVector getTermVector() {
		return termVector;
	}
}
