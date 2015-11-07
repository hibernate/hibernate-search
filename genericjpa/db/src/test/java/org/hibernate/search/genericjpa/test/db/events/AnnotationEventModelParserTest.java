/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.genericjpa.annotations.Hint;
import org.hibernate.search.genericjpa.annotations.IdColumn;
import org.hibernate.search.genericjpa.annotations.IdInfo;
import org.hibernate.search.genericjpa.annotations.UpdateInfo;
import org.hibernate.search.genericjpa.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.db.events.impl.EventModelParser;
import org.hibernate.search.genericjpa.db.IdConverter;
import org.hibernate.search.genericjpa.exception.SearchException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin on 20.07.2015.
 */
public class AnnotationEventModelParserTest {

	EventModelParser parser = new AnnotationEventModelParser();

	@Test
	public void testCorrectUsage() {
		List<EventModelInfo> infos = parser.parse( new HashSet<>( Arrays.asList( SomeEntity.class ) ) );
		Collections.sort(
				infos,
				(first, second) -> first.getOriginalTableName().compareTo( second.getOriginalTableName() )
		);

		{
			EventModelInfo table1Info = infos.get( 0 );
			assertEquals( "table1", table1Info.getOriginalTableName() );
			assertNotEquals( "", table1Info.getUpdateTableName() );
			assertNotEquals( "", table1Info.getEventTypeColumn() );
			assertNotEquals( "", table1Info.getUpdateIdColumn() );

			List<EventModelInfo.IdInfo> idInfos = table1Info.getIdInfos();
			Collections.sort(
					idInfos,
					(first, second) -> first.getColumnsInOriginal()[0].compareTo( second.getColumnsInOriginal()[0] )
			);
			Class<?>[] classes = {SomeEntity.class, SomeOtherEntity.class, YetAnotherEntity.class};
			ColumnType[] columnTypes = {ColumnType.INTEGER, ColumnType.LONG, ColumnType.STRING};
			for ( int i = 0; i < idInfos.size(); ++i ) {
				EventModelInfo.IdInfo cur = idInfos.get( i );
				assertEquals( "column1_" + (i + 1), cur.getColumnsInOriginal()[0] );
				assertTrue( cur.getHints().containsKey( "key" + (i + 1) ) );
				assertEquals( "value" + (i + 1), cur.getHints().get( "key" + (i + 1) ) );
				assertEquals( columnTypes[i], cur.getIdConverter() );
				assertEquals( classes[i], cur.getEntityClass() );
			}
		}

		//the other stuff is just parsed straight-forward. just make sure, we find all the info.
		assertEquals( 4, infos.size() );
	}

	@Test(expected = SearchException.class)
	public void testSameTableTwice() {
		List<EventModelInfo> infos = parser.parse( new HashSet<>( Arrays.asList( SameTableTwice.class ) ) );
		System.err.println( infos );
	}

	@Test(expected = SearchException.class)
	public void testNamingConflictSameAnnotation() {
		List<EventModelInfo> infos = parser.parse( new HashSet<>( Arrays.asList( NamingConflictSameAnnotation.class ) ) );
		System.err.println( infos );
	}

	@Test(expected = SearchException.class)
	public void testNamingConflictTwoAnnotations() {
		List<EventModelInfo> infos = parser.parse( new HashSet<>( Arrays.asList( NamingConflictTwoAnnotations.class ) ) );
		System.err.println( infos );
	}

	@Test
	public void testManualValues() {
		List<EventModelInfo> infos = parser.parse( new HashSet<>( Arrays.asList( ManualValues.class ) ) );

		EventModelInfo info = infos.get( 0 );

		assertEquals( "manualvalues", info.getOriginalTableName() );
		assertEquals( "manualvalues_updates", info.getUpdateTableName() );
		assertEquals( "manualvalues_idcolumn", info.getUpdateIdColumn() );
		assertEquals( "manualvalues_eventtypecolumn", info.getEventTypeColumn() );

		assertEquals( Manual.class, info.getIdInfos().get( 0 ).getEntityClass() );
		assertEquals( "manualcolumn", info.getIdInfos().get( 0 ).getColumnsInOriginal()[0] );
		assertEquals( "manualcolumn_FOREIGN", info.getIdInfos().get( 0 ).getColumnsInUpdateTable()[0] );
		assertTrue(
				info.getIdInfos().get( 0 ).getIdConverter() instanceof ManualIdConverter
		);
	}

	//this information doesn't make a whole lot of sense database wise, but we can test stuff properly still
	@UpdateInfo(tableName = "table1", idInfos = {
			@IdInfo(columns = @IdColumn(column = "column1_1", columnType = ColumnType.INTEGER), hints = @Hint(key = "key1", value = "value1")),
			@IdInfo(entity = SomeOtherEntity.class, columns = @IdColumn(column = "column1_2", columnType = ColumnType.LONG), hints = @Hint(key = "key2", value = "value2")),
			@IdInfo(entity = YetAnotherEntity.class, columns = @IdColumn(column = "column1_3", columnType = ColumnType.STRING), hints = @Hint(key = "key3", value = "value3"))
	})
	@UpdateInfo(tableName = "table2", idInfos = @IdInfo(columns = @IdColumn(column = "column2_1", columnType = ColumnType.INTEGER)))
	public static class SomeEntity {

		@UpdateInfo(tableName = "table3", idInfos = {
				@IdInfo(entity = SomeEntity.class, columns = @IdColumn(column = "column3_1", columnType = ColumnType.INTEGER)),
				@IdInfo(entity = SomeOtherEntity.class, columns = @IdColumn(column = "column3_2", columnType = ColumnType.LONG))
		})
		private Set<SomeOtherEntity> someOtherEntity;

		@UpdateInfo(tableName = "table4", idInfos = {
				@IdInfo(entity = SomeEntity.class, columns = @IdColumn(column = "column4_1", columnType = ColumnType.INTEGER)),
				@IdInfo(entity = SomeOtherEntity.class, columns = @IdColumn(column = "column4_2", columnType = ColumnType.LONG))
		})
		public Set<YetAnotherEntity> getYet() {
			return null;
		}

	}

	public static class SomeOtherEntity {

	}

	public static class YetAnotherEntity {

	}

	@UpdateInfo(tableName = "table_toast", idInfos = @IdInfo(columns = @IdColumn(column = "toast", columnType = ColumnType.INTEGER)))
	@UpdateInfo(tableName = "table_toast", idInfos = @IdInfo(columns = @IdColumn(column = "toast2", columnType = ColumnType.INTEGER)))
	public static class SameTableTwice {

	}

	@UpdateInfo(tableName = "namingconflict", updateTableName = "namingconflict", idInfos = @IdInfo(columns = @IdColumn(column = "toast", columnType = ColumnType.INTEGER)))
	public static class NamingConflictSameAnnotation {

	}

	@UpdateInfo(tableName = "namingconflict", idInfos = @IdInfo(columns = @IdColumn(column = "toast", columnType = ColumnType.INTEGER)))
	@UpdateInfo(tableName = "no_conflict", updateTableName = "namingconflict", idInfos = @IdInfo(columns = @IdColumn(column = "toast", columnType = ColumnType.INTEGER)))
	public static class NamingConflictTwoAnnotations {

	}

	@UpdateInfo(tableName = "manualvalues", updateTableName = "manualvalues_updates", updateTableIdColumn = "manualvalues_idcolumn", updateTableEventTypeColumn = "manualvalues_eventtypecolumn", idInfos = @IdInfo(
			entity = Manual.class, columns = @IdColumn(column = "manualcolumn", updateTableColumn = "manualcolumn_FOREIGN", columnType = ColumnType.INTEGER), idConverter = ManualIdConverter.class
	))
	public static class ManualValues {

	}

	public static class Manual {

	}

	public static class ManualIdConverter implements IdConverter {

		@Override
		public Object convert(Object[] values, String[] fieldNames, ColumnType[] columnTypes) {
			return null;
		}

	}

}
