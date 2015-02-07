/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.IOException;

import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.GroupingManager;
import org.hibernate.search.query.grouping.GroupingRequest;
import org.hibernate.search.query.grouping.GroupingResult;

/**
 * The manager used for all grouping related operation.
 *
 * @author Sascha Grebe
 */
public class GroupingManagerImpl implements GroupingManager {

	private GroupingRequest grouping;

	private GroupingResult groupingResult;

	/**
	 * The query from which this manager was retrieved
	 */
	private final HSQueryImpl query;

	GroupingManagerImpl(HSQueryImpl query) {
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.hibernate.search.query.engine.impl.GroupingManager#group(org.hibernate.search.query.grouping.GroupingRequest)
	 */
	@Override
	public void group(GroupingRequest grouping) {
		this.grouping = grouping;
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.search.query.engine.impl.GroupingManager#getGrouping()
	 */
	@Override
	public GroupingRequest getGrouping() {
		return grouping;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.hibernate.search.query.engine.impl.GroupingManager#setGroupingResult(org.hibernate.search.query.grouping.
	 * GroupingResult)
	 */
	@Override
	public void setGroupingResult(GroupingResult groupingResult) throws IOException {
		this.groupingResult = groupingResult;
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.search.query.engine.impl.GroupingManager#getGroupingResult()
	 */
	@Override
	public GroupingResult getGroupingResult() throws IOException {
		DocumentExtractor queryDocumentExtractor = query.queryDocumentExtractor();
		this.groupingResult.init( queryDocumentExtractor );
		queryDocumentExtractor.close();

		return groupingResult;
	}
}
