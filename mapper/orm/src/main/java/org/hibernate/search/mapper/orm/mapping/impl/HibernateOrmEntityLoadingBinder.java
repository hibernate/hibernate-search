/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityIdEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmNonEntityIdPropertyEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.model.impl.DocumentIdSourceProperty;

public class HibernateOrmEntityLoadingBinder {

	public HibernateOrmEntityLoadingBinder() {
	}

	public <I> HibernateOrmEntityLoadingStrategy<?, ?> createLoadingStrategy(
			PersistentClass persistentClass, DocumentIdSourceProperty<I> documentIdSourceProperty) {
		if ( documentIdSourceProperty != null ) {
			var idProperty = persistentClass.getIdentifierProperty();
			if ( idProperty != null && documentIdSourceProperty.name.equals( idProperty.getName() ) ) {
				return HibernateOrmEntityIdEntityLoadingStrategy
						.create( persistentClass );
			}
			else {
				// The entity ID is not the property used to generate the document ID
				// We need to use a criteria query to load entities from the document IDs
				return HibernateOrmNonEntityIdPropertyEntityLoadingStrategy
						.create( persistentClass, documentIdSourceProperty );
			}
		}
		else {
			// No loading. Can only happen for contained types, which may not be loadable.
			return null;
		}
	}
}
