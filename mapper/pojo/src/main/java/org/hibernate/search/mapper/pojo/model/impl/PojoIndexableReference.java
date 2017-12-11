/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.IndexableReference;

/**
 * @author Yoann Rodiere
 */
public interface PojoIndexableReference<T> extends IndexableReference<T> {

	Class<T> getType();

	T get(Object root);

}
