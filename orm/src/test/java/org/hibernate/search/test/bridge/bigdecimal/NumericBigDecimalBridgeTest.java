/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.bridge.bigdecimal;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.document.Document;
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
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Hardy Ferentschik
 */
@SkipForDialect(value = { SybaseASE15Dialect.class, Sybase11Dialect.class, SQLServer2008Dialect.class },
	comment = "Sybase and MSSQL don't support range large enough for this test")
public class NumericBigDecimalBridgeTest extends SearchTestCase {

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
		List<Item> resultList = (List<Item>) fullTextSession.createFullTextQuery( rootQuery, Item.class ).list();
		assertNotNull( resultList );
		assertTrue( resultList.size() == 1 );

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
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

	public static class BigDecimalNumericFieldBridge extends NumericFieldBridge {
		private static final BigDecimal storeFactor = BigDecimal.valueOf( 100 );

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value != null ) {
				BigDecimal decimalValue = (BigDecimal) value;
				long indexedValue = decimalValue.multiply( storeFactor ).longValue();
				luceneOptions.addNumericFieldToDocument( name, indexedValue, document );
			}
		}

		@Override
		public Object get(String name, Document document) {
			String fromLucene = document.get( name );
			BigDecimal storedBigDecimal = new BigDecimal( fromLucene );
			return storedBigDecimal.divide( storeFactor );
		}
	}
}
