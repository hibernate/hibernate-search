package org.hibernate.search.query.grouping;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;

/**
 * @author Sascha Grebe
 */
public class SimpleGroup implements Group {

	private int totalHits = 0;
	
	private final List<EntityInfo> hits = new LinkedList<>();
	
	private String value;
	
	private ScoreDoc[] scoreDocs;
	
	@Override
	public int getTotalHits() {
		return totalHits;
	}

	@Override
	public List<EntityInfo> getHits() {
		return hits;
	}

	@Override
	public String getValue() {
		return value;
	}

	private void addHit(EntityInfo entityInfo) {
		this.hits.add(entityInfo);
	}
	
	public void setTotalHits(int totalHits) {
		this.totalHits = totalHits;
	}

	public void setScoreDocs(ScoreDoc[] scoreDocs) {
		this.scoreDocs = scoreDocs;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void init(DocumentExtractor extractor) throws IOException {
		for (ScoreDoc nextScoreDoc : scoreDocs) {
			// FIXME I don't think this is the right way to extract the entity infos
			final int index = index(extractor.getTopDocs(), nextScoreDoc);
			final EntityInfo info = extractor.extract(index);
			addHit(info);
		}
	}
	
	private int index(TopDocs topDocs, ScoreDoc scoreDoc) {
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			if (topDocs.scoreDocs[i].doc == scoreDoc.doc) {
				return i;
			}
		}
		return -1;
	}
}
