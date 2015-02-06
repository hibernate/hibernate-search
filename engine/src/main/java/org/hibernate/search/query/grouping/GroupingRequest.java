/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.grouping;

import org.apache.lucene.search.Sort;

/**
 * The grouping request which contains all parameters used for grouping the results.
 * 
 * @author Sascha Grebe
 */
public class GroupingRequest {

	/**
	 * The field name which is used for grouping.
	 */
	private String fieldName;

	/**
	 * The sorting of the groups.
	 */
	private Sort groupSort;

	/**
	 * The sorting withing each group.
	 */
	private Sort withinGroupSort;

	/**
	 * The maximum hits per group.
	 */
	private int maxDocsPerGroup;

	/**
	 * The maximum count of returned groups.
	 */
	private int topGroupCount;

	/**
	 * The count of groups to be skiped.
	 */
	private int groupOffset;

	/**
	 * Indicator whether the total group count shall be calculated. ???This option may be time consuming???
	 */
	private boolean calculateTotalGroupCount;

	/**
	 * The field name which is used for grouping.
	 * 
	 * @return The field name which is used for grouping.
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * The field name which is used for grouping.
	 * 
	 * @param fieldName The field name which is used for grouping.
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * The maximum hits per group.
	 * 
	 * @return The maximum hits per group.
	 */
	public int getMaxDocsPerGroup() {
		return maxDocsPerGroup;
	}

	/**
	 * The maximum hits per group.
	 * 
	 * @param maxDocsPerGroup The maximum hits per group.
	 */
	public void setMaxDocsPerGroup(int maxDocsPerGroup) {
		this.maxDocsPerGroup = maxDocsPerGroup;
	}

	/**
	 * The maximum count of returned groups.
	 * 
	 * @return The maximum count of returned groups.
	 */
	public int getTopGroupCount() {
		return topGroupCount;
	}

	/**
	 * The maximum count of returned groups.
	 * 
	 * @param topGroupCount The maximum count of returned groups.
	 */
	public void setTopGroupCount(int topGroupCount) {
		this.topGroupCount = topGroupCount;
	}

	/**
	 * The sorting of the groups.
	 * 
	 * @return The sorting of the groups.
	 */
	public Sort getGroupSort() {
		return groupSort;
	}

	/**
	 * The sorting of the groups.
	 * 
	 * @param groupSort The sorting of the groups.
	 */
	public void setGroupSort(Sort groupSort) {
		this.groupSort = groupSort;
	}

	/**
	 * The sorting withing each group.
	 * 
	 * @return The sorting withing each group.
	 */
	public Sort getWithinGroupSort() {
		return withinGroupSort;
	}

	/**
	 * The sorting withing each group.
	 * 
	 * @param withinGroupSort The sorting withing each group.
	 */
	public void setWithinGroupSort(Sort withinGroupSort) {
		this.withinGroupSort = withinGroupSort;
	}

	/**
	 * Indicator whether the total group count shall be calculated.
	 * 
	 * @param calculateTotalGroupCount Indicator whether the total group count shall be calculated.
	 */
	public void setCalculateTotalGroupCount(boolean calculateTotalGroupCount) {
		this.calculateTotalGroupCount = calculateTotalGroupCount;
	}

	/**
	 * Indicator whether the total group count shall be calculated.
	 * 
	 * @return Indicator whether the total group count shall be calculated.
	 */
	public boolean isCalculateTotalGroupCount() {
		return calculateTotalGroupCount;
	}

	/**
	 * The count of groups to be skiped.
	 * 
	 * @return The count of groups to be skiped.
	 */
	public int getGroupOffset() {
		return groupOffset;
	}

	/**
	 * The count of groups to be skiped.
	 * 
	 * @param groupOffset The count of groups to be skiped.
	 */
	public void setGroupOffset(int groupOffset) {
		this.groupOffset = groupOffset;
	}

}
