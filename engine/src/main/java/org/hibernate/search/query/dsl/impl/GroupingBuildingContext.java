/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Sort;
import org.hibernate.search.query.grouping.GroupingRequest;

/**
 * @author Sascha Grebe
 */
public class GroupingBuildingContext {

	private String fieldName;

	private Sort groupSort = Sort.RELEVANCE;

	private Sort withinGroupSort = Sort.RELEVANCE;

	private int maxDocsPerGroup = 1;

	private int topGroupCount = 1;

	private int groupOffset = 0;

	private boolean calculateTotalGroupCount = true;

	void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	void setGroupSort(Sort groupSort) {
		this.groupSort = groupSort;
	}

	void setWithinGroupSort(Sort withinGroupSort) {
		this.withinGroupSort = withinGroupSort;
	}

	void setMaxDocsPerGroup(int maxDocsPerGroup) {
		this.maxDocsPerGroup = maxDocsPerGroup;
	}

	void setTopGroupCount(int topGroupCount) {
		this.topGroupCount = topGroupCount;
	}

	void setGroupOffset(int groupOffset) {
		this.groupOffset = groupOffset;
	}

	void setCalculateTotalGroupCount(boolean calculateTotalGroupCount) {
		this.calculateTotalGroupCount = calculateTotalGroupCount;
	}

	GroupingRequest getGroupingRequest() {
		GroupingRequest request = new GroupingRequest();
		request.setFieldName( fieldName );
		request.setGroupOffset( groupOffset );
		request.setGroupSort( groupSort );
		request.setMaxDocsPerGroup( maxDocsPerGroup );
		request.setTopGroupCount( topGroupCount );
		request.setWithinGroupSort( withinGroupSort );
		request.setCalculateTotalGroupCount( calculateTotalGroupCount );

		return request;
	}

}
