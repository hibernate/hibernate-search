/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.apache.lucene.util.BytesRef;
import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializableBinaryField extends SerializableField {

	private byte[] value;

	public SerializableBinaryField(LuceneFieldContext context) {
		super( context );
		final BytesRef binaryValue = context.getBinaryValue();
		final int length = binaryValue.length;
		byte[] extractedBuffer = new byte[length];
		System.arraycopy( binaryValue.bytes, binaryValue.offset, extractedBuffer, 0	, length );
		this.value = extractedBuffer;
	}

	public byte[] getValue() {
		return value;
	}

	public int getOffset() {
		return 0;
	}

	public int getLength() {
		return value.length;
	}
}
