/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.search.util.common.impl.Contracts;

public final class MutableEntityLoadingOptions {
	private int fetchSize;

	private List<EntityGraphHint<?>> entityGraphHints;

	public MutableEntityLoadingOptions(LoadingMappingContext mappingContext) {
		this.fetchSize = mappingContext.fetchSize();
	}

	public int fetchSize() {
		return fetchSize;
	}

	public void fetchSize(int fetchSize) {
		Contracts.assertStrictlyPositive( fetchSize, "fetchSize" );
		this.fetchSize = fetchSize;
	}

	public EntityGraphHint<?> entityGraphHintOrNullForType(EntityMappingType entityMappingType) {
		if ( entityGraphHints == null || entityGraphHints.isEmpty() ) {
			return null;
		}
		EntityMappingType testedType = entityMappingType;
		while ( testedType != null ) {
			for ( EntityGraphHint<?> entityGraphHint : entityGraphHints ) {
				// This cast is fine because a RootGraph always applies to an entity type
				EntityDomainType<?> graphedType = (EntityDomainType<?>) entityGraphHint.graph.getGraphedType();
				if ( graphedType.getHibernateEntityName().equals( testedType.getEntityName() ) ) {
					return entityGraphHint;
				}
			}
			testedType = testedType.getSuperMappingType();
		}
		return null;
	}

	public void entityGraphHint(EntityGraphHint<?> entityGraphHint, boolean replaceExisting) {
		if ( entityGraphHints == null ) {
			entityGraphHints = new ArrayList<>();
		}
		else if ( replaceExisting ) {
			entityGraphHints.clear();
		}
		if ( entityGraphHint == null ) {
			return;
		}
		this.entityGraphHints.add( entityGraphHint );
	}
}
