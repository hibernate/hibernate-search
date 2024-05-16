/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference.impl;

import org.hibernate.search.engine.search.reference.ObjectFieldReference;

public class ObjectFieldReferenceImpl implements ObjectFieldReference {

	private final String absolutePath;

	public ObjectFieldReferenceImpl(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}
}
