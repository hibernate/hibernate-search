/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.search.bridge.spi.FieldType;

/**
 * A field defined by a bridge.
 *
 * @author Gunnar Morling
 */
public class BridgeDefinedField {

	private String name;
	private FieldType type;

	public BridgeDefinedField(String name, FieldType type) {
		this.name = name;
		this.type = type;
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
