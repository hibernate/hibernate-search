/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.triggers;

import java.util.Locale;

import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.EventType;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Implementation of a {@link TriggerSQLStringSource} that can be used with MySQL (or compatible) Databases. <br>
 * <br>
 * In order to provide uniqueness between the Update tables it uses a procedure that generates unique ids. This
 * procedure does this with auxilliary table that only has a autoincrement id. A row is inserted everytime a unique id
 * is needed and that id is retrieved via MySQLs last_insert_id() and then returned
 * <br>
 * <br>
 * We don't escape the column names that come from the EventModelInfos
 * as we don't have any control over how these are defined
 *
 * @author Martin Braun
 */
public class MySQLTriggerSQLStringSource implements TriggerSQLStringSource {

	public static final String DEFAULT_UNIQUE_ID_TABLE_NAME = "_____unique____id____hsearch";
	public static final String DEFAULT_UNIQUE_ID_PROCEDURE_NAME = "get_unique_id_hsearch";

	private static final String CREATE_TRIGGER_ORIGINAL_TABLE_SQL_FORMAT = "" + "CREATE TRIGGER `%s` AFTER %s ON %s                 \n"
			+ "FOR EACH ROW                                                                                                       \n"
			+ "BEGIN                                                                                                              \n"
			+ "    CALL `%s`(@unique_id);                                                                                           \n"
			+ "    INSERT INTO `%s`(`%s`, `%s`, %s)                                                                                     \n"
			+ "		VALUES(@unique_id, %s, %s);                                                                                   \n"
			+ "END;                                                                                                               \n";
	private static final String CREATE_TRIGGER_CLEANUP_SQL_FORMAT = "" + "CREATE TRIGGER `%s` AFTER DELETE ON `%s`                    \n"
			+ "FOR EACH ROW                                                                                                       \n"
			+ "BEGIN                                                                                                              \n"
			+ "DELETE FROM #UNIQUE_ID_TABLE_NAME# WHERE id = OLD.`#updatetableidcolumn#`;                                           \n"
			+ "END;                                                                                                               \n";
	private static final String DROP_TRIGGER_SQL_FORMAT = "" + "DROP TRIGGER IF EXISTS `%s`;\n";

	private final String uniqueIdTableName;
	private final String uniqueIdProcedureName;

	// we don't support dropping the unique_id_table_name
	// because otherwise we would lose information about the last used
	// ids
	private String createTriggerCleanUpSQLFormat;
	private String createUniqueIdTable;
	private String dropUniqueIdTable;
	private String dropUniqueIdProcedure;
	private String createUniqueIdProcedure;

	public MySQLTriggerSQLStringSource() {
		this( DEFAULT_UNIQUE_ID_TABLE_NAME, DEFAULT_UNIQUE_ID_PROCEDURE_NAME );
	}

	public MySQLTriggerSQLStringSource(String uniqueIdTableName, String uniqueIdProcedureName) {
		this.uniqueIdTableName = uniqueIdTableName;
		this.uniqueIdProcedureName = uniqueIdProcedureName;
		this.init();
	}

	private void init() {
		this.createUniqueIdTable = String.format(
				(Locale) null,
				"CREATE TABLE IF NOT EXISTS `%s` (                                                 \n"
						+ "`id` BIGINT(64) NOT NULL AUTO_INCREMENT,                                                                          \n"
						+ "PRIMARY KEY (id)                                                                                               \n"
						+ ");                                                                                                              \n",
				this.uniqueIdTableName
		);
		this.dropUniqueIdTable = String.format( (Locale) null, "DROP TABLE IF EXISTS `%s`;", this.uniqueIdTableName );
		this.dropUniqueIdProcedure = String.format(
				(Locale) null,
				"DROP PROCEDURE IF EXISTS `%s`;                                                  \n",
				this.uniqueIdProcedureName
		);
		this.createUniqueIdProcedure = String.format(
				(Locale) null,
				"CREATE PROCEDURE `%s`                                                         \n"
						+ "(OUT ret BIGINT)                                                                                                \n"
						+ "BEGIN                                                                                                           \n"
						+ "	INSERT INTO `%s` VALUES ();                                                                                      \n"
						+ "	SET ret = last_insert_id();                                                                                    \n"
						+ "END;                                                                                                            \n",
				this.uniqueIdProcedureName, this.uniqueIdTableName
		);
		this.createTriggerCleanUpSQLFormat = CREATE_TRIGGER_CLEANUP_SQL_FORMAT.replaceAll(
				"#UNIQUE_ID_TABLE_NAME#",
				this.uniqueIdTableName
		);
	}

	@Override
	public String[] getUnSetupCode() {
		return new String[] {this.dropUniqueIdProcedure, this.dropUniqueIdTable};
	}

	@Override
	public String[] getSetupCode() {
		return new String[] {this.createUniqueIdTable, this.createUniqueIdProcedure};
	}

	@Override
	public String[] getTriggerCreationCode(EventModelInfo eventModelInfo, int eventType) {
		String originalTableName = eventModelInfo.getOriginalTableName();
		String triggerName = this.getTriggerName( eventModelInfo.getOriginalTableName(), eventType );
		String tableName = eventModelInfo.getUpdateTableName();
		String eventTypeColumn = eventModelInfo.getEventTypeColumn();
		StringBuilder valuesFromOriginal = new StringBuilder();
		StringBuilder idColumnNames = new StringBuilder();
		int addedVals = 0;
		for ( EventModelInfo.IdInfo idInfo : eventModelInfo.getIdInfos() ) {
			for ( int i = 0; i < idInfo.getColumnsInUpdateTable().length; ++i ) {
				if ( addedVals > 0 ) {
					valuesFromOriginal.append( ", " );
					idColumnNames.append( ", " );
				}
				if ( eventType == EventType.DELETE ) {
					valuesFromOriginal.append( "OLD." );
				}
				else {
					valuesFromOriginal.append( "NEW." );
				}
				valuesFromOriginal.append( idInfo.getColumnsInOriginal()[i] );
				idColumnNames.append( "`" + idInfo.getColumnsInUpdateTable()[i] + "`" );
				++addedVals;
			}
		}
		if ( addedVals == 0 ) {
			throw new IllegalArgumentException( "eventModelInfo didn't contain any idInfos" );
		}
		String eventTypeValue = String.valueOf( eventType );
		String createTriggerOriginalTableSQL = new StringBuilder().append(
				String.format(
						(Locale) null,
						CREATE_TRIGGER_ORIGINAL_TABLE_SQL_FORMAT,
						triggerName,
						EventType.toString( eventType ),
						originalTableName,
						this.uniqueIdProcedureName,
						tableName,
						eventModelInfo.getUpdateIdColumn(),
						eventTypeColumn,
						idColumnNames.toString(),
						eventTypeValue,
						valuesFromOriginal.toString()
				)
		)
				.toString();
		return new String[] {createTriggerOriginalTableSQL};
	}

	@Override
	public String[] getTriggerDropCode(EventModelInfo eventModelInfo, int eventType) {
		String triggerName = this.getTriggerName( eventModelInfo.getOriginalTableName(), eventType );
		return new String[] {
				String.format( (Locale) null, DROP_TRIGGER_SQL_FORMAT, triggerName )
						.toUpperCase( Locale.ROOT )
		};
	}

	private String getTriggerName(String originalTableName, int eventType) {
		return new StringBuilder().append( originalTableName ).append( "_updates_hsearch_" ).append(
				EventType.toString(
						eventType
				)
		).toString();
	}

	private String getCleanUpTriggerName(String updatesTableName) {
		return new StringBuilder().append( updatesTableName ).append( "_cleanup_hsearch" ).toString();
	}

	@Override
	public String[] getSpecificSetupCode(EventModelInfo eventModelInfo) {
		String createTriggerCleanUpSQL = String.format(
				(Locale) null,
				this.createTriggerCleanUpSQLFormat.replaceAll(
						"#updatetableidcolumn#",
						eventModelInfo.getUpdateIdColumn()
				), this.getCleanUpTriggerName( eventModelInfo.getUpdateTableName() ),
				eventModelInfo.getUpdateTableName()
		);
		return new String[] {createTriggerCleanUpSQL};
	}

	@Override
	public String[] getSpecificUnSetupCode(EventModelInfo eventModelInfo) {
		return new String[] {
				String.format(
						(Locale) null,
						DROP_TRIGGER_SQL_FORMAT,
						this.getCleanUpTriggerName( eventModelInfo.getUpdateTableName() )
				)
		};
	}

	@Override
	public String[] getUpdateTableCreationCode(EventModelInfo info) {
		String tableName = info.getUpdateTableName();
		String updateIdColumn = info.getUpdateIdColumn();
		String eventTypeColumn = info.getEventTypeColumn();
		String sql =
				"CREATE TABLE IF NOT EXISTS `" + tableName + "` (\n" +
						"    `" + updateIdColumn + "` BIGINT(64) NOT NULL,\n" +
						"    `" + eventTypeColumn + "` INT NOT NULL,\n";
		for ( EventModelInfo.IdInfo idInfo : info.getIdInfos() ) {
			String[] columnsInUpdateTable = idInfo.getColumnsInUpdateTable();
			ColumnType[] columnTypes = idInfo.getColumnTypes();
			String[] columnDefinitions = idInfo.getColumnDefinitions();
			for ( int i = 0; i < columnsInUpdateTable.length; ++i ) {
				String columnDefinition = columnDefinitions[i];
				if ( "".equals( columnDefinition ) ) {
					columnDefinition = toMySQLType( columnTypes[i] );
				}
				sql += "    `" + columnsInUpdateTable[i] + "` " + columnDefinition + " NOT NULL,\n";
			}
		}
		sql += "    PRIMARY KEY (`" + updateIdColumn + "`)\n" +
				");";
		return new String[] {
				sql
		};
	}

	private static String toMySQLType(ColumnType columnType) {
		switch ( columnType ) {
			case INTEGER:
				return "INT";
			case LONG:
				return "BIGINT(64)";
			case STRING:
				return "VARCHAR(255)";
			default:
				throw new AssertionFailure( "unexpected columnType: " + columnType );
		}
	}

	@Override
	public String[] getUpdateTableDropCode(EventModelInfo info) {
		return new String[] {
				String.format( (Locale) null, "DROP TABLE IF EXISTS `%s`;", info.getUpdateTableName() )
		};
	}

	@Override
	public String getDelimitedIdentifierToken() {
		return "`";
	}

}
