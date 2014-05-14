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
 * A message is made of:
 * - a protocol version number
 * - a set of operations
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */

public class Message implements Serializable {
	private Set<Operation> operations;

	public Message(Set<Operation> operations) {
		this.operations = operations;
	}

	public Set<Operation> getOperations() {
		return operations;
	}
}
