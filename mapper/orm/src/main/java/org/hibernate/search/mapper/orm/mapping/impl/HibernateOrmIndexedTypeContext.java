/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeIndexedTypeContext<E> {
	private final PojoMappingTypeMetadata metadata;

	private HibernateOrmIndexedTypeContext(Builder<E> builder) {
		super( builder.javaClass );
		this.metadata = builder.metadata;
	}

	@Override
	public PojoMappingTypeMetadata getMappingMetadata() {
		return metadata;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private final Class<E> javaClass;

		private PojoMappingTypeMetadata metadata;

		Builder(Class<E> javaClass) {
			this.javaClass = javaClass;
		}

		@Override
		public void metadata(PojoMappingTypeMetadata metadata) {
			this.metadata = metadata;
		}

		public HibernateOrmIndexedTypeContext<E> build() {
			return new HibernateOrmIndexedTypeContext<>( this );
		}
	}
}
