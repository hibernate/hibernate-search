/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.io.Serializable;

public class TransientReference<T> implements Serializable {

	private final transient T value;

	public TransientReference(T value) {
		this.value = value;
	}

	public T get() {
		return value;
	}
}
