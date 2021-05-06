/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.mapping.metadata.impl.JavaBeanEntityTypeMetadata;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanContainedTypeContext<E> extends AbstractJavaBeanTypeContext<E> {

	private JavaBeanContainedTypeContext(Builder<E> builder) {
		super( builder );
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoContainedTypeExtendedMappingCollector {
		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName, JavaBeanEntityTypeMetadata<E> metadata) {
			super( typeIdentifier, entityName, metadata );
		}

		JavaBeanContainedTypeContext<E> build() {
			return new JavaBeanContainedTypeContext<>( this );
		}
	}
}
