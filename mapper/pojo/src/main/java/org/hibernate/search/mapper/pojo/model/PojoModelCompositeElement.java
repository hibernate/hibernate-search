/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

import java.util.stream.Stream;

/**
 * A potentially composite element in the POJO model.
 * <p>
 * Offers ways to create {@link PojoElementAccessor accessors} allowing
 * to retrieve data from objects passed to bridges.
 *
 * @see PojoModelType
 * @see PojoModelProperty
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface PojoModelCompositeElement extends PojoModelElement {

	<T> PojoElementAccessor<T> createAccessor(Class<T> type);

	PojoElementAccessor<?> createAccessor();

	PojoModelProperty property(String relativeFieldName);

	Stream<? extends PojoModelProperty> properties();

}
