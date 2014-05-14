/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SerializableDocument implements Serializable {

	private Set<SerializableFieldable> fieldables;

	public SerializableDocument(Set<SerializableFieldable> fieldables) {
		this.fieldables = fieldables;
	}

	public Set<SerializableFieldable> getFieldables() {
		return fieldables;
	}

}
