/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

// FIXME HSEARCH-3203 Replace this temporary solution with an object-oriented one.
public final class PojoMappingTypeMetadata {

	private final boolean documentIdMappedToEntityId;

	public PojoMappingTypeMetadata(boolean documentIdMappedToEntityId) {
		this.documentIdMappedToEntityId = documentIdMappedToEntityId;
	}

	public boolean isDocumentIdMappedToEntityId() {
		return documentIdMappedToEntityId;
	}

}
