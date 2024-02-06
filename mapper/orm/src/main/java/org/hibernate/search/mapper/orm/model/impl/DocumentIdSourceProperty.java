/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class DocumentIdSourceProperty<I> {
	public final Class<? super I> clazz;
	public final String name;
	public final ValueReadHandle<I> handle;

	public DocumentIdSourceProperty(PojoPropertyModel<I> documentIdSourceProperty) {
		this.clazz = documentIdSourceProperty.typeModel().rawType().typeIdentifier().javaClass();
		this.name = documentIdSourceProperty.name();
		this.handle = documentIdSourceProperty.handle();
	}
}
