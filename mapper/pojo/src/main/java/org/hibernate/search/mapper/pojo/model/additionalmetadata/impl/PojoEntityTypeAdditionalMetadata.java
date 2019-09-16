/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;

public class PojoEntityTypeAdditionalMetadata {
	private final String entityName;
	private final PojoPathFilterFactory<Set<String>> pathFilterFactory;
	private final Optional<String> entityIdPropertyName;

	public PojoEntityTypeAdditionalMetadata(String entityName,
			PojoPathFilterFactory<Set<String>> pathFilterFactory,
			Optional<String> entityIdPropertyName) {
		this.entityName = entityName;
		this.pathFilterFactory = pathFilterFactory;
		this.entityIdPropertyName = entityIdPropertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	/**
	 * @return A path filter factory for this type.
	 */
	public PojoPathFilterFactory<Set<String>> getPathFilterFactory() {
		return pathFilterFactory;
	}

	public Optional<String> getEntityIdPropertyName() {
		return entityIdPropertyName;
	}
}
