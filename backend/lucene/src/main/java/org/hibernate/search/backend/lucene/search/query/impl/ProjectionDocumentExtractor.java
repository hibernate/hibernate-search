/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

interface ProjectionDocumentExtractor {

	void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder);

	void contributeFields(Set<String> absoluteFieldPaths);

	void extract(ProjectionHitCollector collector, ScoreDoc scoreDoc, Document document);
}
