package org.hibernate.search.processor.model;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

public class MyEmbeddedEntity {

	@KeywordField
	private String keyword;

	@IndexedEmbedded(includeDepth = 3, excludePaths = { "embedded" })
	private MyIndexedEntity child;

}
