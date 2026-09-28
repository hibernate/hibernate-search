package org.hibernate.search.mapper.pojo.massindexing;

import java.util.OptionalLong;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface MassIndexingTypeGroupMonitorContext {

	/**
	 * Provides a total count of entities within the type group that should be indexed if obtaining such count is possible.
	 * <p>
	 * <b>Warning</b>: This operation is not cached and a count from the underlying loading strategy
	 * will be requested on each call to get the total count.
	 * <p>
	 * The loaders used to calculate the count provided by this context are reused by the
	 * indexing process, which means that, in general, the number returned by this context
	 * should match the number of entities to index.
	 *
	 * @return The total count of entities to be indexed within the current type group, or an empty optional
	 * if the count cannot be determined by the underlying loading strategy, e.g. when the strategy is based on a stream data
	 * and obtaining count is not possible until all elements of the stream are consumed.
	 */
	OptionalLong totalCount();

}
