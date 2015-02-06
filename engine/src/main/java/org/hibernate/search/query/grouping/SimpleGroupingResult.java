/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.grouping;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.query.engine.spi.DocumentExtractor;

/**
 * @author Sascha Grebe
 */
public class SimpleGroupingResult implements GroupingResult {

	private List<Group> groups = new LinkedList<>();

	private Integer totalGroupCount;

	private int totalGroupedHitCount = 0;

	private int totalHitCount = 0;

	@Override
	public Integer getTotalGroupCount() {
		return totalGroupCount;
	}

	public void setTotalGroupCount(Integer totalGroupCount) {
		this.totalGroupCount = totalGroupCount;
	}

	@Override
	public List<Group> getGroups() {
		return groups;
	}

	public void addGroup(Group group) {
		this.groups.add( group );
	}

	@Override
	public int getTotalGroupedHitCount() {
		return totalGroupedHitCount;
	}

	public void setTotalGroupedHitCount(int totalGroupedHitCount) {
		this.totalGroupedHitCount = totalGroupedHitCount;
	}

	@Override
	public int getTotalHitCount() {
		return totalHitCount;
	}

	public void setTotalHitCount(int totalHitCount) {
		this.totalHitCount = totalHitCount;
	}

	@Override
	public void init(DocumentExtractor extractor) throws IOException {
		for ( Group nextGroup : groups ) {
			nextGroup.init( extractor );
		}
	}

}
