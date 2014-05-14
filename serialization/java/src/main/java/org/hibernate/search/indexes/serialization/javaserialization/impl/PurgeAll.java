/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PurgeAll implements Operation {
	private String entityClassName;

	public PurgeAll(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public String getEntityClassName() {
		return entityClassName;
	}
}
