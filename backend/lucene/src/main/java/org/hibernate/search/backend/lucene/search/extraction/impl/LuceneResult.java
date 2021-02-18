/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.apache.lucene.document.Document;

public class LuceneResult {

	private final Document document;

	private final int docId;

	private final float score;

	public LuceneResult(Document document, int docId, float score) {
		this.document = document;
		this.docId = docId;
		this.score = score;
	}

	public Document getDocument() {
		return document;
	}

	public int getDocId() {
		return docId;
	}

	public float getScore() {
		return score;
	}
}
