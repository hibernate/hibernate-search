/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;

class HibernateOrmContainedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeContainedTypeContext<E> {
	private final PojoMappingTypeMetadata metadata;

	private HibernateOrmContainedTypeContext(HibernateOrmContainedTypeContext.Builder<E> builder) {
		super( builder.javaClass );
		this.metadata = builder.metadata;
	}

	@Override
	public PojoMappingTypeMetadata getMappingMetadata() {
		return metadata;
	}

	static class Builder<E> implements PojoContainedTypeExtendedMappingCollector {
		private final Class<E> javaClass;

		private PojoMappingTypeMetadata metadata;

		Builder(Class<E> javaClass) {
			this.javaClass = javaClass;
		}

		@Override
		public void metadata(PojoMappingTypeMetadata metadata) {
			this.metadata = metadata;
		}

		HibernateOrmContainedTypeContext<E> build() {
			return new HibernateOrmContainedTypeContext<>( this );
		}
	}
}
