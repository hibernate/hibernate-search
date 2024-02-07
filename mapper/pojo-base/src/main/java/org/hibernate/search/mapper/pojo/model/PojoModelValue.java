/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A model element representing a value bound to a bridge.
 *
 * @see org.hibernate.search.mapper.pojo.bridge.ValueBridge
 */
@Incubating
public interface PojoModelValue<T> extends PojoModelElement {

}
