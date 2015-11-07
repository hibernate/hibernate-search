/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.triggers;

import java.util.Locale;

import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.EventType;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.exception.AssertionFailure;

/**
 * We don't escape the column names that come from the EventModelInfos
 * as we don't have any control over how these are defined
 *
 * @author Martin
 */
public class HSQLDBTriggerSQLStringSource implements TriggerSQLStringSource {


	private static final String UNIQUE_ID_SEQUENCE_NAME = "unique___id___hsearch";
	private static final String CREATE_UNIQUE_ID_SEQUENCE_SQL = "CREATE SEQUENCE \"" + UNIQUE_ID_SEQUENCE_NAME + "\" AS BIGINT START WITH 1 INCREMENT BY 1;";
	private static final String DROP_UNIQUE_ID_SEQUENCE_SQL = "DROP SEQUENCE \"" + UNIQUE_ID_SEQUENCE_NAME + "\"";

	private static final String DROP_TRIGGER_FORMAT_SQL = "DROP TRIGGER \"%s\"";

	private static final String TRIGGER_CREATION_FORMAT_INSERT_UPDATE = "CREATE TRIGGER \"%s\" AFTER %s ON %s\n" +
			"REFERENCING NEW AS \"newrow\"\n" +
			"FOR EACH ROW\n" +
			"BEGIN ATOMIC\n" +
			"        DECLARE nextupdateid BIGINT;\n" +
			"        SET nextupdateid = NEXT VALUE FOR \"" + UNIQUE_ID_SEQUENCE_NAME + "\";\n" +
			"        INSERT INTO \"%s\"(\"%s\", \"%s\", %s) \n" +
			"                VALUES (nextupdateid, %s, %s);\n" +
			"END;";

	private static final String TRIGGER_CREATION_FORMAT_DELETE = "CREATE TRIGGER \"%s\" AFTER %s ON %s\n" +
			"REFERENCING OLD AS \"oldrow\"\n" +
			"FOR EACH ROW\n" +
			"BEGIN ATOMIC\n" +
			"        DECLARE nextupdateid BIGINT;\n" +
			"        SET nextupdateid = NEXT VALUE FOR \"" + UNIQUE_ID_SEQUENCE_NAME + "\";\n" +
			"        INSERT INTO \"%s\"(\"%s\", \"%s\", %s) \n" +
			"                VALUES (nextupdateid, %s, %s);\n" +
			"END;";

	@Override
	public String[] getUnSetupCode() {
		return new String[] {
				DROP_UNIQUE_ID_SEQUENCE_SQL
		};
	}

	@Override
	public String[] getSetupCode() {
		return new String[] {
				CREATE_UNIQUE_ID_SEQUENCE_SQL
		};
	}

	@Override
	public String[] getSpecificSetupCode(EventModelInfo eventModelInfo) {
		//not needed here
		return new String[0];
	}

	@Override
	public String[] getSpecificUnSetupCode(EventModelInfo eventModelInfo) {
		//not needed here
		return new String[0];
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
					valuesFromOriginal.append( "\"oldrow\"." );
				}
				else {
					valuesFromOriginal.append( "\"newrow\"." );
				}
				valuesFromOriginal.append( idInfo.getColumnsInOriginal()[i] );
				idColumnNames.append( "\"" + idInfo.getColumnsInUpdateTable()[i] + "\"" );
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
						eventType == EventType.DELETE ?
								TRIGGER_CREATION_FORMAT_DELETE :
								TRIGGER_CREATION_FORMAT_INSERT_UPDATE,
						triggerName,
						EventType.toString( eventType ),
						originalTableName,
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
		return new String[] {
				String.format(
						(Locale) null,
						DROP_TRIGGER_FORMAT_SQL, this.getTriggerName(
								eventModelInfo.getOriginalTableName(),
								eventType
						)
				)
		};
	}

	@Override
	public String[] getUpdateTableCreationCode(EventModelInfo info) {
		String tableName = info.getUpdateTableName();
		String updateIdColumn = info.getUpdateIdColumn();
		String eventTypeColumn = info.getEventTypeColumn();
		String sql =
				"CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (\n" +
						"    \"" + updateIdColumn + "\" BIGINT NOT NULL,\n" +
						"    \"" + eventTypeColumn + "\" INT NOT NULL,\n";
		for ( EventModelInfo.IdInfo idInfo : info.getIdInfos() ) {
			String[] columnsInUpdateTable = idInfo.getColumnsInUpdateTable();
			ColumnType[] columnTypes = idInfo.getColumnTypes();
			String[] columnDefinitions = idInfo.getColumnDefinitions();
			for ( int i = 0; i < columnsInUpdateTable.length; ++i ) {
				String columnDefinition = columnDefinitions[i];
				if ( "".equals( columnDefinition ) ) {
					columnDefinition = toMySQLType( columnTypes[i] );
				}
				sql += "    \"" + columnsInUpdateTable[i] + "\" " + columnDefinition + " NOT NULL,\n";
			}
		}
		sql += "    PRIMARY KEY (\"" + updateIdColumn + "\")\n" +
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
				return "BIGINT";
			case STRING:
				return "VARCHAR(255)";
			default:
				throw new AssertionFailure( "unexpected columnType: " + columnType );
		}
	}

	@Override
	public String[] getUpdateTableDropCode(EventModelInfo info) {
		return new String[] {
				String.format((Locale) null, "DROP TABLE IF EXISTS \"%s\";", info.getUpdateTableName() )
		};
	}

	@Override
	public String getDelimitedIdentifierToken() {
		return "\"";
	}

	private String getTriggerName(String originalTableName, int eventType) {
		return new StringBuilder().append( originalTableName ).append( "_updates_hsearch_" ).append(
				EventType.toString(
						eventType
				)
		).toString();
	}

}
