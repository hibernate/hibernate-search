/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;

public class LuceneChildrenCollector implements Collector {

	private final Map<String, Set<Integer>> children = new HashMap<>();
	private static final Set<String> ROOT_ID_FIELD_SET = Collections.singleton( LuceneFields.rootIdFieldName() );

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		return new FieldLeafCollector( context );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public Map<String, Set<Integer>> getChildren() {
		return children;
	}

	private class FieldLeafCollector implements LeafCollector {

		private final LeafReader reader;

		public FieldLeafCollector(LeafReaderContext context) throws IOException {
			reader = context.reader();
		}

		@Override
		public void setScorer(Scorable scorer) throws IOException {
			// we don't need any scorer
		}

		@Override
		public void collect(int doc) throws IOException {
			// FIXME tried to use BinaryDocValues without success. The field isn't nether binary nor numeric.
			String parentId = reader.document( doc, ROOT_ID_FIELD_SET ).getField( LuceneFields.rootIdFieldName() ).stringValue();
			if ( !children.containsKey( parentId ) ) {
				children.put( parentId, new HashSet<>() );
			}
			children.get( parentId ).add( doc );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "LuceneChildrenCollector{" );
		sb.append( "children=" ).append( children );
		sb.append( '}' );
		return sb.toString();
	}
}
