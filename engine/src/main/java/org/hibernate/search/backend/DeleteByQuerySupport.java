package org.hibernate.search.backend;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * This class has means to convert all (by default) supported
 * DeletionQueries back to Lucene Queries and to their
 * String[] representation and back.
 * 
 * @author Martin Braun
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
				public Query build(DeletionQuery deleteByQuery,
						ScopedAnalyzer analyzerForEntity) {
					SingularTermQuery query = (SingularTermQuery) deleteByQuery;
					try {
						TokenStream tokenStream = analyzerForEntity
								.tokenStream(query.getFieldName(),
										query.getValue());
						tokenStream.reset();
						try {
							BooleanQuery booleanQuery = new BooleanQuery();
							while (tokenStream.incrementToken()) {
								String value = tokenStream.getAttribute(
										CharTermAttribute.class).toString();
								booleanQuery.add(
										new TermQuery(new Term(query
												.getFieldName(), value)),
										Occur.MUST);
							}
							return booleanQuery;
						} finally {
							tokenStream.close();
						}
					} catch (IOException e) {
						throw new AssertionError(
								"no IOException can occur while using a TokenStream "
										+ "that is generated via String");
					}
				}

			});

			TO_LUCENE_QUERY_CONVERTER = Collections.unmodifiableMap(map);
		}
	}

	public static final Map<Integer, Class<? extends DeletionQuery>> SUPPORTED_TYPES;
	static {
		{
			Map<Integer, Class<? extends DeletionQuery>> map = new HashMap<>();

			map.put(SingularTermQuery.QUERY_KEY, SingularTermQuery.class);

			SUPPORTED_TYPES = Collections.unmodifiableMap(map);
		}
	}

	public static final Map<Integer, StringToQueryMapper> FROM_STRING;
	static {
		{
			Map<Integer, StringToQueryMapper> map = new HashMap<>();

			map.put(SingularTermQuery.QUERY_KEY, new StringToQueryMapper() {

				@Override
				public DeletionQuery fromString(String[] string) {
					if (string.length != 2) {
						throw new IllegalArgumentException(
								"for a TermQuery to work there have to be "
										+ "exactly 2 Arguments (fieldName & value");
					}
					return new SingularTermQuery(string[0], string[1]);
				}

			});

			FROM_STRING = Collections.unmodifiableMap(map);
		}
	}

	public static final Map<Integer, QueryToStringMapper> TO_STRING;
	static {
		{
			Map<Integer, QueryToStringMapper> map = new HashMap<>();

			map.put(SingularTermQuery.QUERY_KEY, new QueryToStringMapper() {

				@Override
				public String[] toString(DeletionQuery deleteByQuery) {
					SingularTermQuery query = (SingularTermQuery) deleteByQuery;
					return new String[] { query.getFieldName(),
							query.getValue() };
				}

			});

			TO_STRING = Collections.unmodifiableMap(map);
		}
	}

	static {
		//make sure everything is setup correctly
		Set<Integer> counts = new HashSet<>();
		counts.add(TO_LUCENE_QUERY_CONVERTER.size());
		counts.add(TO_STRING.size());
		counts.add(FROM_STRING.size());
		counts.add(SUPPORTED_TYPES.size());
		if (counts.size() != 1) {
			throw new AssertionError(
					"all Maps/Sets inside this class must have the same "
							+ "size. Make sure that every QueryType is found in every Map/Set");
		}
	}

	/**
	 * Interface to map the several <code>DeleteByQuery</code>s to a Lucene
	 * query.
	 * 
	 * This is done outside of the <code>DeleteByQuery</code> class to not
	 * overcomplicate the serialization process
	 * 
	 * @author Martin Braun
	 */
	public static interface ToLuceneQuery {

		public Query build(DeletionQuery deleteByQuery,
				ScopedAnalyzer analyzerForEntity);

	}

	/*
	 * Why we use String arrays here:
	 * 
	 * Why use Strings at all and not i.e. byte[] ?: As we want to support
	 * different versions of Queries later on and we don't want to write
	 * Serialization code over and over again in the Serialization module for
	 * all the different types we have one toString and fromString here. and we
	 * force to use Strings because i.e. using byte[] would suggest using Java
	 * Serialization for this process, but that would prevent us from changing
	 * our API internally in the future.
	 * 
	 * Why not plain Strings?:
	 * 
	 * But using plain Strings would leave us with yet another problem: We would
	 * have to encode all our different fields into a single string. For that we
	 * would need some magical chars to separate or we would need to use
	 * something like JSON/XML to pass the data consistently. This would be far
	 * too much overkill for us.
	 * 
	 * By using String[] we force users (or API designers) to no use Java
	 * Serialization (well Serialization to BASE64 or another String
	 * representation is still possible, but nvm that) and we still have an
	 * _easy_ way of dealing with multiple fields in our different Query Types.
	 */

	public static interface StringToQueryMapper {

		public DeletionQuery fromString(String[] string);

	}

	public static interface QueryToStringMapper {

		public String[] toString(DeletionQuery deleteByQuery);

	}

}
