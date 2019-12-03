/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Optional;

/**
 * An abstract base for implementations of {@link PojoGenericTypeModel}.
 */
public abstract class AbstractPojoGenericTypeModel<T> implements PojoGenericTypeModel<T> {

	private final PojoRawTypeModel<? super T> rawTypeModel;

	protected AbstractPojoGenericTypeModel(PojoRawTypeModel<? super T> rawTypeModel) {
		this.rawTypeModel = rawTypeModel;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + getName() + "]";
	}

	@Override
	public final PojoRawTypeModel<? super T> getRawType() {
		return rawTypeModel;
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		return rawTypeModel.getProperty( propertyName );
	}

	@Override
	public Optional<? extends PojoGenericTypeModel<?>> getTypeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		return Optional.empty();
	}

	@Override
	public Optional<? extends PojoGenericTypeModel<?>> getArrayElementType() {
		return Optional.empty();
	}
}
