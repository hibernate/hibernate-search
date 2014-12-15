package org.hibernate.search.backend.impl.lucene.works;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.SerializableQuery;
import org.hibernate.search.backend.SingularTermQuery;

/**
 * 
 * @author Martin
 *
 */
public final class DeleteByQuerySupport {
	
	private DeleteByQuerySupport() {
		throw new AssertionError("can't touch this!");
	}
	
	public static final Map<Integer, ToLuceneQuery> TO_LUCENE_QUERY_CONVERTER;
	static {
		{
			Map<Integer, ToLuceneQuery> map = new HashMap<>();

			map.put(SingularTermQuery.QUERY_KEY, new ToLuceneQuery() {

				@Override
				public Query build(SerializableQuery serializableQuery) {
					SingularTermQuery query = (SingularTermQuery) serializableQuery;
					return new TermQuery(new Term(query.getKey(), query
							.getValue()));
				}

			});

			TO_LUCENE_QUERY_CONVERTER = Collections.unmodifiableMap(map);
		}
	}
	
	public static final Map<Integer, Class<? extends SerializableQuery>> SUPPORTED_TYPES;
	static {
		{
			Map<Integer, Class<? extends SerializableQuery>> map = new HashMap<>();

			map.put(SingularTermQuery.QUERY_KEY, SingularTermQuery.class);

			SUPPORTED_TYPES = Collections.unmodifiableMap(map);
		}
	}

}
