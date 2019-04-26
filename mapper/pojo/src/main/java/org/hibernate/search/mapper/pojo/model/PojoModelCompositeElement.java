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
 *    You should be prepared for incompatible changes in future releases.
 */
public interface PojoModelCompositeElement extends PojoModelElement {

	// FIXME what if I want a PojoElementAccessor<List<MyType>>?
	<T> PojoElementAccessor<T> createAccessor(Class<T> type);

	PojoElementAccessor<?> createAccessor();

	PojoModelProperty property(String relativeFieldName);

	Stream<? extends PojoModelProperty> properties();

}
