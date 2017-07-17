/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;


/**
 * @author Yoann Rodiere
 */
public class EntityTypeDescriptor {

	private final Class<?> javaClass;

	private final IdOrder idOrder;

	public EntityTypeDescriptor(Class<?> javaClass, IdOrder idOrder) {
		super();
		this.javaClass = javaClass;
		this.idOrder = idOrder;
	}

	public Class<?> getJavaClass() {
		return javaClass;
	}

	public IdOrder getIdOrder() {
		return idOrder;
	}

}
