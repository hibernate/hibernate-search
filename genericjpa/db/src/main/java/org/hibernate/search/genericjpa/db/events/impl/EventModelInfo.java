/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.IdConverter;

/**
 * contains information about the EventModel. Instances of this class can be obtained by a {@link EventModelParser}.
 *
 * @author Martin
 */
public class EventModelInfo {

	private final String updateTableName;
	private final String originalTableName;
	private final String eventTypeColumn;
	private final List<IdInfo> idInfos;
	private final String updateIdColumn;

	public EventModelInfo(
			String tableName,
			String originalTableName,
			String eventTypeColumn, String updateIdColumn,
			List<IdInfo> idInfos) {
		this.updateTableName = tableName;
		this.originalTableName = originalTableName;
		this.eventTypeColumn = eventTypeColumn;
		this.idInfos = idInfos;
		this.updateIdColumn = updateIdColumn;
	}

	public String getUpdateIdColumn() {
		return updateIdColumn;
	}

	/**
	 * @return the updateTableName
	 */
	public String getUpdateTableName() {
		return updateTableName;
	}

	/**
	 * @return the originalTableName
	 */
	public String getOriginalTableName() {
		return originalTableName;
	}

	/**
	 * @return the idInfos
	 */
	public List<IdInfo> getIdInfos() {
		return idInfos;
	}

	/**
	 * @return the eventTypeColumn
	 */
	public String getEventTypeColumn() {
		return eventTypeColumn;
	}

	@Override
	public String toString() {
		return "EventModelInfo{" +
				"updateTableName='" + updateTableName + '\'' +
				", originalTableName='" + originalTableName + '\'' +
				", eventTypeColumn='" + eventTypeColumn + '\'' +
				", idInfos=" + idInfos +
				", updateIdColumn='" + updateIdColumn + '\'' +
				'}';
	}

	public static class IdInfo {

		private final Class<?> entityClass;
		private final String[] columnsInUpdateTable;
		private final String[] columnsInOriginal;
		private final ColumnType[] columnTypes;
		private final String[] columnDefinitions;
		private final IdConverter idConverter;
		private final Map<String, String> hints;

		public IdInfo(
				Class<?> entityClass,
				String[] columnsInUpdateTable,
				String[] columnsInOriginal,
				ColumnType[] columnTypes,
				String[] columnDefinitions,
				IdConverter idConverter, Map<String, String> hints) {
			this.entityClass = entityClass;
			this.columnsInUpdateTable = columnsInUpdateTable;
			this.idConverter = idConverter;
			this.hints = hints;
			this.columnsInOriginal = columnsInOriginal;
			this.columnTypes = columnTypes;
			this.columnDefinitions = columnDefinitions;
		}

		public String[] getColumnDefinitions() {
			return columnDefinitions;
		}

		public ColumnType[] getColumnTypes() {
			return columnTypes;
		}

		public IdConverter getIdConverter() {
			return idConverter;
		}

		/**
		 * @return the entityClass
		 */
		public Class<?> getEntityClass() {
			return entityClass;
		}

		/**
		 * @return the columnsInUpdateTable
		 */
		public String[] getColumnsInUpdateTable() {
			return columnsInUpdateTable;
		}

		/**
		 * @return the columnsInOriginal
		 */
		public String[] getColumnsInOriginal() {
			return columnsInOriginal;
		}

		public Map<String, String> getHints() {
			return hints;
		}

		@Override
		public String toString() {
			return "IdInfo{" +
					"entityClass=" + entityClass +
					", columnsInUpdateTable=" + Arrays.toString( columnsInUpdateTable ) +
					", columnsInOriginal=" + Arrays.toString( columnsInOriginal ) +
					", columnTypes=" + Arrays.toString( columnTypes ) +
					", idConverter=" + idConverter +
					", hints=" + hints +
					'}';
		}
	}

}
