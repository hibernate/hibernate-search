/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * @author Yoann Rodiere
 */
public interface IndexableModel {

	// FIXME what if I want a IndexableReference<List<MyType>>?
	<T> IndexableReference<T> asReference(Class<T> type);

	IndexableReference<?> asReference();

	boolean isAssignableTo(Class<?> clazz);

	<M extends Annotation> Stream<M> markers(Class<M> markerType);

	IndexableModel property(String relativeName);

	Stream<IndexableModel> properties();

}
