/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <T> The type used as a root element.
 */
public class PojoModelValueElement<T> implements PojoModelValue<T> {

	private final PojoGenericTypeModel<? extends T> valueTypeModel;
	private final PojoRawTypeModel<? extends T> valueRawTypeModel;

	public PojoModelValueElement(PojoGenericTypeModel<? extends T> valueTypeModel,
			PojoRawTypeModel<? extends T> valueRawTypeModel) {
		this.valueTypeModel = valueTypeModel;
		this.valueRawTypeModel = valueRawTypeModel;
	}

	@Override
	public String toString() {
		return "PojoModelValueElement[" + valueTypeModel.toString() + "]";
	}

	@Override
	public Class<? extends T> getRawType() {
		return valueRawTypeModel.getJavaClass();
	}
}