/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.triggers;

import java.util.Locale;

import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.EventType;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Created by Martin on 30.06.2015.
 */
public class PostgreSQLTriggerSQLStringSource implements TriggerSQLStringSource {

	private static final String UNIQUE_ID_SEQUENCE_NAME = "______unique_id_hsearch_____";
	private static final String CREATE_UNIQUE_ID_SEQUENCE_SQL = "DO $$BEGIN \n" +
			"    CREATE SEQUENCE \"" + UNIQUE_ID_SEQUENCE_NAME + "\";\n" +
			"    EXCEPTION WHEN duplicate_table \n" +
			"    THEN RAISE NOTICE 'Hibernate Search Unique ID Sequence already exists.';\n" +
			"END;$$;";

	private static final String DROP_SEQUENCE_SQL = "DO $$BEGIN\n" +
			"    DROP SEQUENCE \"" + UNIQUE_ID_SEQUENCE_NAME + "\";\n" +
			"    EXCEPTION WHEN undefined_table \n" +
			"    THEN RAISE NOTICE 'Hibernate Search Unique ID Sequence did not exist.';\n" +
			"END;$$;";

	private static final String DROP_TRIGGER_FORMAT_SQL = "DROP TRIGGER IF EXISTS \"%s\" ON %s";
	private static final String DROP_FUNCTION_FORMAT_SQL = "DROP FUNCTION IF EXISTS \"%s\"();";

	private static final String CREATE_FUNCTION_FORMAT_SQL = "CREATE OR REPLACE FUNCTION \"%s\"() RETURNS TRIGGER AS $$\n" +
			"    BEGIN\n" +
			"        INSERT INTO \"%s\"(\"%s\", \"%s\", %s)\n" +
			"        VALUES(nextval('" + UNIQUE_ID_SEQUENCE_NAME + "'), %s, %s);\n" +
			"        RETURN NEW;\n" +
			"    END\n" +
			"$$ LANGUAGE plpgsql;";
	private static final String CREATE_TRIGGER_FORMAT_SQL = "CREATE TRIGGER \"%s\"\n" +
			"    AFTER %s ON %s\n" +
			"    FOR EACH ROW\n" +
			"    EXECUTE PROCEDURE \"%s\"();";

	@Override
	public String[] getUnSetupCode() {
		return new String[] {
				DROP_SEQUENCE_SQL
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
		String functionName = this.getFunctionName(
				eventModelInfo.getOriginalTableName(),
				eventType
		);
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
				idColumnNames.append( "\"" + idInfo.getColumnsInUpdateTable()[i] + "\"" );
				++addedVals;
			}
		}
		if ( addedVals == 0 ) {
			throw new IllegalArgumentException( "eventModelInfo didn't contain any idInfos" );
		}
		String eventTypeValue = String.valueOf( eventType );
		return new String[] {
				String.format(
						(Locale) null,
						CREATE_FUNCTION_FORMAT_SQL,
						functionName,
						tableName,
						eventModelInfo.getUpdateIdColumn(),
						eventTypeColumn,
						idColumnNames.toString(),
						eventTypeValue,
						valuesFromOriginal.toString()
				),
				String.format(
						(Locale) null,
						CREATE_TRIGGER_FORMAT_SQL,
						triggerName,
						EventType.toString( eventType ),
						originalTableName,
						functionName
				)
		};
	}

	@Override
	public String[] getTriggerDropCode(EventModelInfo eventModelInfo, int eventType) {
		return new String[] {
				String.format(
						(Locale) null,
						DROP_TRIGGER_FORMAT_SQL, this.getTriggerName(
								eventModelInfo.getOriginalTableName(),
								eventType
						), eventModelInfo.getOriginalTableName()
				),
				String.format(
						(Locale) null,
						DROP_FUNCTION_FORMAT_SQL, this.getFunctionName(
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

	private String getFunctionName(String originalTableName, int eventType) {
		return new StringBuilder().append( originalTableName ).append( "_updates_hsearch_function_" ).append(
				EventType.toString(
						eventType
				)
		).toString();
	}

}
