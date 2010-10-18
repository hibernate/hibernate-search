package org.hibernate.search.test.engine;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.SearchTestCase;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;


public class NumericFieldTest extends SearchTestCase {


	private List numericQueryFor(String fieldName, FullTextSession session, Object from, Object to) {
		NumericRangeQuery query = null;
		if(from instanceof Double) {
			query = NumericRangeQuery.newDoubleRange(fieldName,(Double)from,(Double)to,true,true);
		 }
		if(from instanceof Long) {
			query = NumericRangeQuery.newLongRange(fieldName,(Long)from,(Long)to,true,true);
		 }
		if(from instanceof Integer) {
			query = NumericRangeQuery.newIntRange(fieldName,(Integer)from,(Integer)to,true,true);
		 }
		if(from instanceof Float) {
			query = NumericRangeQuery.newFloatRange(fieldName,(Float)from,(Float)to,true,true);
		 }
		return session.createFullTextQuery(query, Location.class).list();

	}

	public void testIndexAndSearchLongNumericField() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.save(new Location(3L, 4L, 0.5510d, 56.12d, 34, "Random text", 45.56d));
		tx.commit();
		session.clear();

		FullTextSession fts = Search.getFullTextSession(session);
		tx = session.beginTransaction();

		assertEquals("Query by double range", 1, numericQueryFor( "latitude",fts,0.55d,0.552d ).size() );
		assertEquals("Query by integer range", 1, numericQueryFor("ranking",fts,30,40).size() );
		assertEquals("Query by long range", 1, numericQueryFor("myCounter",fts,1L,5L).size() );
		assertEquals("Query by double multiple", 1, numericQueryFor("strMultiple",fts,45.4d, 45.6d).size() );


		TermQuery termQuery = new TermQuery(new Term("description", "text"));

		List result = fts.createFullTextQuery(termQuery, Location.class).list();
		assertEquals(1, result.size());


		
		tx.commit();
		fts.close();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Location.class};
	}

}


@Entity
@Indexed
class Location {

	@Id
	private int id;

	@Field(name = "myCounter") @NumericField(forField = "myCounter")
	private Long counter;

	@Field(store = Store.YES)
	private Long sku;

	@Field(store = Store.YES) @NumericField(forField = "latitude", precisionStep = 1)
	private Double latitude;

	@NumericField(forField = "longitude")
	@Field(store = Store.YES)
	private Double longitude;

	@NumericField
	@Field(store = Store.YES)
	private Integer ranking;

	@Field
	private String description;

	@Fields({
		@Field(name="strMultiple"),
		@Field
	})
	@NumericFields({
		@NumericField(forField = "strMultiple")
	})
	private Double multiple;

	public Location() {
	}

	public Location(Long counter, long sku, Double latitude, Double longitude, Integer ranking, String description, Double multiple) {
		this.counter = counter;
		this.sku = sku;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ranking = ranking;
		this.description = description;
		this.multiple = multiple;
	}

}