/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * A field defined by a bridge.
 *
 * @author Gunnar Morling
 */
public class BridgeDefinedField {

	private final String name;
	private final FieldType type;
	private final Index index;

	public BridgeDefinedField(String name, FieldType type, Index index) {
		this.name = name;
		this.type = type;
		this.index = index;
	}

	public Field.Index getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public FieldType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "BridgeDefinedField [name=" + name + ", type=" + type + "]";
	}
}
