/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.search.reference.impl.ObjectFieldReferenceImpl;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The field reference representing an object.
 */
@Incubating
public interface ObjectFieldReference extends FieldReference {

	static ObjectFieldReference of(String path) {
		return new ObjectFieldReferenceImpl( path );
	}

}
