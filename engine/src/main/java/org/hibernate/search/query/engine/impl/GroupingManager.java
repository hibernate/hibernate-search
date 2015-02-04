package org.hibernate.search.query.engine.impl;

import java.io.IOException;

import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.grouping.GroupingRequest;
import org.hibernate.search.query.grouping.GroupingResult;

/**
 * The manager used for all grouping related operation.
 * 
 * @author Sascha Grebe
 */
public class GroupingManager {

	private GroupingRequest grouping;
	
	private GroupingResult groupingResult;
	
	/**
	 * The query from which this manager was retrieved
	 */
	private final HSQueryImpl query;
	
	GroupingManager(HSQueryImpl query) {
		this.query = query;
	}
	
	public void group(GroupingRequest grouping) {
		this.grouping = grouping;
	}
	
	public GroupingRequest getGrouping() {
		return grouping;
	}
	
	public void setGroupingResult(GroupingResult groupingResult) throws IOException {
		this.groupingResult = groupingResult;
	}
	
	public GroupingResult getGroupingResult() throws IOException {
		DocumentExtractor queryDocumentExtractor = query.queryDocumentExtractor();
		this.groupingResult.init(queryDocumentExtractor);
		queryDocumentExtractor.close();
		
		return groupingResult;
	}
}
