/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.triggers.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.triggers.impl.ORMEventModelParser;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.entities.Domain;
import org.hibernate.search.test.entities.Embedded;
import org.hibernate.search.test.entities.OverrideEntity;
import org.hibernate.search.test.entities.OverrideEntityCustomType;
import org.hibernate.search.test.entities.Place;
import org.hibernate.search.test.entities.SecondaryTableEntity;
import org.hibernate.search.test.entities.SingleTable;
import org.hibernate.search.test.entities.SingleTableOne;
import org.hibernate.search.test.entities.Sorcerer;
import org.hibernate.search.test.entities.TablePerClass;
import org.hibernate.search.test.entities.TablePerClassOne;
import org.hibernate.search.test.entities.TablePerClassTwo;
import org.hibernate.search.test.entities.TopLevel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Martin Braun
 */
public class ORMEventModelParserTest extends SearchTestBase {

	@Test
	public void testSingularEntityMapping() {
		ORMEventModelParser parser = new ORMEventModelParser( this.getSessionFactory(), set( Domain.class ) );
		List<EventModelInfo> eventModelInfos = parser.parse( Collections.singletonList( Domain.class ) );

		Map<String, EventModelInfo> map = toMap( eventModelInfos );

		this.assertInfos( map, Domain.class, ColumnType.INTEGER, "DOMAIN", "id" );

		assertEquals( 1, eventModelInfos.size() );
	}

	@Test
	public void testSingleTableInheritance() {
		ORMEventModelParser parser = new ORMEventModelParser( this.getSessionFactory(), set( SingleTableOne.class ) );
		List<EventModelInfo> eventModelInfos = parser.parse( Collections.singletonList( SingleTableOne.class ) );

		Map<String, EventModelInfo> map = toMap( eventModelInfos );
		this.assertInfos( map, SingleTable.class, ColumnType.INTEGER, "SingleTable", "id" );

		assertEquals( 1, eventModelInfos.size() );
	}

	@Test
	public void testEmbeddedMapping() {
		ORMEventModelParser parser = new ORMEventModelParser(
				this.getSessionFactory(), set(
				TopLevel.class,
				Embedded.class
		)
		);

		List<EventModelInfo> eventModelInfos = parser.parse( Arrays.asList( TopLevel.class, Embedded.class ) );

		Map<String, EventModelInfo> map = toMap( eventModelInfos );

		this.assertInfos( map, TopLevel.class, ColumnType.STRING, "toplevel", "id" );
		this.assertInfos( map, TopLevel.class, ColumnType.STRING, "toplevel_embedded", "id" );

		assertEquals( 2, eventModelInfos.size() );
	}

	@Test
	public void testBasicOneToManyMapping() {
		ORMEventModelParser parser = new ORMEventModelParser(
				this.getSessionFactory(), set(
				Place.class,
				Sorcerer.class
		)
		);
		List<EventModelInfo> eventModelInfos = parser.parse( Arrays.asList( Place.class, Sorcerer.class ) );

		Map<String, EventModelInfo> map = toMap( eventModelInfos );
		this.assertInfos( map, Sorcerer.class, ColumnType.INTEGER, "Sorcerer", "id" );
		this.assertInfos( map, Place.class, ColumnType.INTEGER, "Place", "id" );
		this.assertInfos( map, Sorcerer.class, ColumnType.INTEGER, "Sorcerer_Place", "sorcerer_id" );

		assertEquals( 3, eventModelInfos.size() );
	}

	@Test
	public void testSecondaryTableMapping() {
		ORMEventModelParser parser = new ORMEventModelParser(
				this.getSessionFactory(),
				set( SecondaryTableEntity.class )
		);

		List<EventModelInfo> eventModelInfos = parser.parse( Collections.singletonList( SecondaryTableEntity.class ) );
		Map<String, EventModelInfo> map = toMap( eventModelInfos );

		this.assertInfos( map, SecondaryTableEntity.class, ColumnType.LONG, "PRIME", "ID" );
		this.assertInfos( map, SecondaryTableEntity.class, ColumnType.LONG, "SECONDARY", "SEC_ID" );

		assertEquals( 2, eventModelInfos.size() );
	}

	@Test
	public void testOverrideMapping() {
		ORMEventModelParser parser = new ORMEventModelParser(
				this.getSessionFactory(),
				set( OverrideEntity.class ),
				new AnnotationEventModelParser()
		);

		List<EventModelInfo> eventModelInfos = parser.parse( Collections.singletonList( OverrideEntity.class ) );
		Map<String, EventModelInfo> map = toMap( eventModelInfos );

		EventModelInfo sorcerer = map.get( "ENTITY" );
		assertEquals( "ENTITY", sorcerer.getOriginalTableName() );
		assertEquals( "ENTITY_UPDATES", sorcerer.getUpdateTableName() );
		assertEquals( 1, sorcerer.getIdInfos().size() );
		assertEquals( "UPDATE_TABLE_ID", sorcerer.getIdInfos().get( 0 ).getColumnsInUpdateTable()[0] );

		//this is enough, the rest is tested in the Annotation Parser
		//we just check whether the overridden settings were chosen

		assertEquals( 1, eventModelInfos.size() );
	}

	@Test
	public void testOnlyRelevant() {
		{
			ORMEventModelParser parser = new ORMEventModelParser( this.getSessionFactory(), set() );
			List<EventModelInfo> eventModelInfos = parser.parse( Arrays.asList( this.getAnnotatedClasses() ) );
			assertEquals( 0, eventModelInfos.size() );
		}

		{
			ORMEventModelParser parser = new ORMEventModelParser( this.getSessionFactory(), set( Sorcerer.class ) );
			List<EventModelInfo> eventModelInfos = parser.parse( Arrays.asList( this.getAnnotatedClasses() ) );
			assertEquals( 2, eventModelInfos.size() );
		}
	}

	@Test
	public void testTablePerClass() {
		{
			ORMEventModelParser parser = new ORMEventModelParser(
					this.getSessionFactory(), set(
					TablePerClass.class,
					TablePerClassOne.class,
					TablePerClassTwo.class
			)
			);
			List<EventModelInfo> eventModelInfos = parser.parse(
					Arrays.asList(
							TablePerClass.class,
							TablePerClassOne.class,
							TablePerClassTwo.class
					)
			);
			assertEquals( 3, eventModelInfos.size() );

			Map<String, EventModelInfo> map = toMap( eventModelInfos );

			this.assertInfos( map, TablePerClass.class, ColumnType.INTEGER, "TablePerClass", "ID" );
			this.assertInfos( map, TablePerClass.class, ColumnType.INTEGER, "TablePerClassOne", "ID" );
			this.assertInfos( map, TablePerClass.class, ColumnType.INTEGER, "TablePerClassTwo", "ID" );
		}
	}

	@Test
	public void testOverrideEntityId() {
		{
			ORMEventModelParser parser = new ORMEventModelParser(
					this.getSessionFactory(), set(
					OverrideEntityCustomType.class
			)
			);
			List<EventModelInfo> eventModelInfos = parser.parse(
					Arrays.asList(
							OverrideEntityCustomType.class
					)
			);
			assertEquals( 1, eventModelInfos.size() );

			Map<String, EventModelInfo> map = toMap( eventModelInfos );

			this.assertInfos(
					map,
					OverrideEntityCustomType.class,
					ColumnType.INTEGER,
					"OverrideEntityCustomType",
					"ID"
			);
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				TopLevel.class,
				Embedded.class,
				Domain.class,
				Place.class,
				Sorcerer.class,
				SecondaryTableEntity.class,
				TablePerClass.class,
				TablePerClassOne.class,
				TablePerClassTwo.class,
				OverrideEntity.class,
				OverrideEntityCustomType.class,
				SingleTable.class,
				SingleTableOne.class
		};
	}

	private static <T> Set<T> set(T... values) {
		if ( values.length == 0 ) {
			return Collections.emptySet();
		}
		return new HashSet<>( Arrays.asList( values ) );
	}

	private void assertInfos(
			Map<String, EventModelInfo> map,
			Class<?> entityClass,
			ColumnType columnType,
			String tableName,
			String idColumnName) {
		EventModelInfo evi = map.get( tableName );
		assertEquals( ORMEventModelParser.DEFAULT_EVENT_TYPE_COLUMN, evi.getEventTypeColumn() );
		assertEquals( ORMEventModelParser.DEFAULT_UPDATE_ID_COLUMN, evi.getUpdateIdColumn() );
		assertEquals( tableName, evi.getOriginalTableName() );
		assertEquals( tableName + ORMEventModelParser.DEFAULT_HSEARCH_UPDATES_SUFFIX, evi.getUpdateTableName() );

		assertEquals( 1, evi.getIdInfos().size() );
		EventModelInfo.IdInfo idInfo = evi.getIdInfos().get( 0 );
		assertEquals( idColumnName, idInfo.getColumnsInOriginal()[0] );
		assertEquals(
				idColumnName + ORMEventModelParser.DEFAULT_HSEARCH_UPDATES_SUFFIX,
				idInfo.getColumnsInUpdateTable()[0]
		);
		assertEquals( entityClass, idInfo.getEntityClass() );
		if ( columnType != ColumnType.CUSTOM && idInfo.getColumnTypes().length == 1 ) {
			assertEquals( columnType, idInfo.getIdConverter() );
		}
		else {
			assertNotNull( idInfo.getIdConverter() );
		}
		assertEquals( columnType, idInfo.getColumnTypes()[0] );
		assertEquals( 0, idInfo.getHints().size() );
		assertEquals( "", idInfo.getColumnDefinitions()[0] );
	}

	private static Map<String, EventModelInfo> toMap(Collection<EventModelInfo> collection) {
		Map<String, EventModelInfo> ret = new HashMap<>( collection.size() );
		for ( EventModelInfo evi : collection ) {
			ret.put( evi.getOriginalTableName(), evi );
		}
		return ret;
	}

}
