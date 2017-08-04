/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

/**
 * @author Yoann Rodiere
 */
/*
 * TODO discuss the usefulness of the indexable model.
 *
 * Theoretically, we could skip it entirely during binding
 * and only pass java.lang.Object to the bridges when indexing.
 *
 * But if we did:
 *
 * 1. We wouldn't be able to know in advance which properties
 * of the object are going to be read, which would prevent
 * some future optimizations (when reading streams for instance,
 * or when handling ContainedIns).
 * 2. And more importantly, the bridge wouldn't be able to
 * inspect additional markers on the indexed type, such as
 * @Latitude and @Longitude for spatial bridges for instance.
 *
 * Maybe a good compromise would be to use indexable models
 * during binding, but only for type introspection, and
 * to pass java.lang.Object to the bridges when indexing?
 */
public interface IndexableModel {

	// FIXME what if I want a IndexableReference<List<MyType>>?
	<T> IndexableReference<T> asReference(Class<T> type);

	IndexableReference<?> asReference();

	IndexableModel property(String relativeName);

}
