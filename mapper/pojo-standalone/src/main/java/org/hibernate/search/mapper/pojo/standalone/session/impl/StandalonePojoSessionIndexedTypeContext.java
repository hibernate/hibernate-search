/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * @param <E> The entity type mapped to the index.
 */
public interface StandalonePojoSessionIndexedTypeContext<E> {

	PojoRawTypeIdentifier<E> typeIdentifier();

	String name();

}
