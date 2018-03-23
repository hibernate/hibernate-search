/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.impl;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.extractor.spi.ContainerValueExtractorPath;

public class PojoAssociationPath {
	private final String propertyName;
	private final ContainerValueExtractorPath extractorPath;

	public PojoAssociationPath(String propertyName, ContainerValueExtractorPath extractorPath) {
		this.propertyName = propertyName;
		this.extractorPath = extractorPath;
	}

	@Override
	public String toString() {
		return propertyName + extractorPath;
	}

	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof PojoAssociationPath ) ) {
			return false;
		}
		PojoAssociationPath other = (PojoAssociationPath) obj;
		return propertyName.equals( other.propertyName )
				&& extractorPath.equals( other.extractorPath );
	}

	@Override
	public int hashCode() {
		return Objects.hash( propertyName, extractorPath );
	}

	public String getPropertyName() {
		return propertyName;
	}

	public ContainerValueExtractorPath getExtractorPath() {
		return extractorPath;
	}
}
