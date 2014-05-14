/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;
import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializableTokenStreamField extends SerializableField {

	private SerializableTokenStream value;
	private SerializableTermVector termVector;

	public SerializableTokenStreamField(LuceneFieldContext context) {
		super( context );
		this.value = context.getTokenStream();
		this.termVector = context.getTermVector();
	}

	public SerializableTokenStream getValue() {
		return value;
	}

	public SerializableTermVector getTermVector() {
		return termVector;
	}
}
