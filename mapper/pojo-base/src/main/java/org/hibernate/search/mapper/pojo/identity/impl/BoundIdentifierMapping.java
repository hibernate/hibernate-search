/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class BoundIdentifierMapping<I, E> {
	public final IdentifierMappingImplementor<I, E> mapping;
	public final PojoTypeModel<I> identifierType;
	public final Optional<PojoPropertyModel<I>> documentIdSourceProperty;

	public BoundIdentifierMapping(IdentifierMappingImplementor<I, E> mapping, PojoTypeModel<I> identifierType,
			Optional<PojoPropertyModel<I>> documentIdSourceProperty) {
		this.mapping = mapping;
		this.identifierType = identifierType;
		this.documentIdSourceProperty = documentIdSourceProperty;
	}

	public IdentifierMappingImplementor<I, E> mapping() {
		return mapping;
	}
}
