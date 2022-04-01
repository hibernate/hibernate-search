/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;

public final class PojoConstructorModelFormatter {

	private final PojoConstructorModel<?> constructorModel;

	public PojoConstructorModelFormatter(PojoConstructorModel<?> constructorModel) {
		this.constructorModel = constructorModel;
	}

	@Override
	public String toString() {
		return constructorModel.typeModel().name() + "("
				+ CommaSeparatedClassesFormatter.format( constructorModel.parametersJavaTypes() ) + ")";
	}
}
