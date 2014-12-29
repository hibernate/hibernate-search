/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

/**
 *
 * @author Martin Braun
 */
public class DeleteByQuery implements Operation {

	private String entityClassName;
	private int key;
	private String[] query;

	public DeleteByQuery(String entityClassName, int key, String[] query) {
		this.key = key;
		this.query = query;
	}

	public int getKey() {
		return this.key;
	}

	public String[] getQuery() {
		return this.query;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

}
