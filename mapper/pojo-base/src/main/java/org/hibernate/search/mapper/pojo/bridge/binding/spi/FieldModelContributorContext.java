/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.spi;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;

public interface FieldModelContributorContext {

	void indexNullAs(String value);

	StandardIndexFieldTypeOptionsStep<?, ?> standardTypeOptionsStep();

	StringIndexFieldTypeOptionsStep<?> stringTypeOptionsStep();

	ScaledNumberIndexFieldTypeOptionsStep<?, ?> scaledNumberTypeOptionsStep();

	VectorFieldTypeOptionsStep<?, ?> vectorTypeOptionsStep();

	void checkNonStandardTypeOptionsStep();
}
