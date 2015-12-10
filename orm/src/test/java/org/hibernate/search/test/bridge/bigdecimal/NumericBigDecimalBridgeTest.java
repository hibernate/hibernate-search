/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.bigdecimal;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
@SkipForDialect(value = { SybaseASE15Dialect.class, Sybase11Dialect.class, SQLServer2008Dialect.class },
	comment = "Sybase and MSSQL don't support range large enough for this test")
public class NumericBigDecimalBridgeTest extends SearchTestBase {

	@Test
	public void testNumericFieldWithBigDecimals() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		// create entities
		Item item = new Item();
		item.setPrice( new BigDecimal( 154.34 ) );
		session.save( item );

		tx.commit();

		tx = session.beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Item.class )
				.get();

		Query rootQuery = queryBuilder.bool()
				.must( queryBuilder.range().onField( "price" ).above( 10000l ).createQuery() )
				.must( queryBuilder.range().onField( "price" ).below( 20000l ).createQuery() )
				.createQuery();

		@SuppressWarnings( "unchecked" )
		List<Item> resultList = fullTextSession.createFullTextQuery( rootQuery, Item.class ).list();
		assertNotNull( resultList );
		assertEquals( 1, resultList.size() );

		tx.commit();
		session.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Item.class };
	}

	@Entity
	@Indexed
	@Table(name = "ITEM")
	public static class Item {
		@Id
		@GeneratedValue
		private int id;

		@Field
		@NumericField
		@FieldBridge(impl = BigDecimalNumericFieldBridge.class)
		private BigDecimal price;

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public int getId() {
			return id;
		}
	}

	public static class BigDecimalNumericFieldBridge implements MetadataProvidingFieldBridge, TwoWayFieldBridge {
		private static final BigDecimal storeFactor = BigDecimal.valueOf( 100 );

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value == null ) {
				if ( luceneOptions.indexNullAs() != null ) {
					luceneOptions.addFieldToDocument( name, luceneOptions.indexNullAs(), document );
				}
			}
			else {
				BigDecimal decimalValue = (BigDecimal) value;
				long indexedValue = decimalValue.multiply( storeFactor ).longValue();
				luceneOptions.addNumericFieldToDocument( name, indexedValue, document );
			}
		}

		@Override
		public Object get(String name, Document document) {
			final IndexableField field = document.getField( name );
			if ( field != null ) {
				Number numericValue = field.numericValue();
				BigDecimal bigValue = new BigDecimal( numericValue.longValue() );
				return bigValue.divide( storeFactor );
			}
			else {
				return null;
			}
		}

		@Override
		public final String objectToString(final Object object) {
			return object == null ? null : String.valueOf( object );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( name, FieldType.LONG );
		}
	}
}
