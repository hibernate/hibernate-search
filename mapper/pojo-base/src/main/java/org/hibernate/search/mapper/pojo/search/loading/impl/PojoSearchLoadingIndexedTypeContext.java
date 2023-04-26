/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.loading.impl;

import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;

public interface PojoSearchLoadingIndexedTypeContext<E> extends PojoLoadingTypeContext<E> {

	String entityName();

	IdentifierMapping identifierMapping();

}
