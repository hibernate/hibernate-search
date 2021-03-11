/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import java.io.Serializable;

@SuppressWarnings("unused")
class CustomBoundedGenericType<T extends Number & Cloneable & Serializable> implements CustomBoundedGenericInterface<T> {
}
