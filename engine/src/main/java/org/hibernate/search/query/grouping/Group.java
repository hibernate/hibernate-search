package org.hibernate.search.query.grouping;

import java.io.IOException;
import java.util.List;

import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;

/**
 * Contains the values of a single group.
 * 
 * @author Sascha Grebe
 */
public interface Group {

	/**
	 * @return The total number of elements in the group.
	 */
	int getTotalHits();
	
	/**
	 * @return The matching entity infos.
	 */
	List<EntityInfo> getHits();
	
	/**
	 * @return The value which is used for grouping.
	 */
	String getValue();
	
	/**
	 * Extract the entity infos from the found documents.
	 * @param extractor The document extractor.
	 * @throws IOException
	 */
	void init(DocumentExtractor extractor) throws IOException;
	
}
