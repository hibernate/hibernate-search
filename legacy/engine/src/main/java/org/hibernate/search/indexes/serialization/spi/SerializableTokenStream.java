/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.util.AttributeImpl;

/**
 * @author Emmanuel Bernard
 */
public class SerializableTokenStream implements Serializable {
	private List<List<AttributeImpl>> stream;

	public SerializableTokenStream(List<List<AttributeImpl>> stream) {
		this.stream = stream;
	}

	public List<List<AttributeImpl>> getStream() {
		return stream;
	}
}
