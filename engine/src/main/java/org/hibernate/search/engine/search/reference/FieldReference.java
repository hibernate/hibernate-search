/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The most common interface for the field reference hierarchy.
 * @param <SR> Containing type.
 */
@Incubating
public interface FieldReference<SR> {

	String absolutePath();

	Class<SR> scopeRootType();

}
