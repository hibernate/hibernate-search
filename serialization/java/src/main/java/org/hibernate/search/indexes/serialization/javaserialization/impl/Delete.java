/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;


import java.io.Serializable;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Delete implements Operation {
	private String entityClassName;
	private Serializable id;

	public Delete(String entityClassName, Serializable id) {
		this.entityClassName = entityClassName;
		this.id = id;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public Serializable getId() {
		return id;
	}
}
