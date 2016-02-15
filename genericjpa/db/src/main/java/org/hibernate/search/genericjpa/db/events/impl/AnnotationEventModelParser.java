/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.genericjpa.annotations.Hint;
import org.hibernate.search.genericjpa.annotations.IdColumn;
import org.hibernate.search.genericjpa.annotations.IdInfo;
import org.hibernate.search.genericjpa.annotations.UpdateInfo;
import org.hibernate.search.genericjpa.db.ColumnType;
import org.hibernate.search.genericjpa.db.IdConverter;
import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;

/**
 * Created by Martin on 20.07.2015.
 */
public class AnnotationEventModelParser implements EventModelParser {

	@Override
	public List<EventModelInfo> parse(Set<Class<?>> updateClasses) {
		ArrayList<Class<?>> l = new ArrayList<>( updateClasses.size() );
		l.addAll( updateClasses );
		return this.parse( l );
	}

	@Override
	public List<EventModelInfo> parse(List<Class<?>> updateClasses) {
		List<EventModelInfo> ret = new ArrayList<>( updateClasses.size() );
		Set<String> handledOriginalTableNames = new HashSet<>( updateClasses.size() );
		Set<String> updateTableNames = new HashSet<>( updateClasses.size() );
		for ( Class<?> clazz : updateClasses ) {
			{
				UpdateInfo[] classUpdateInfos = clazz.getAnnotationsByType( UpdateInfo.class );
				this.addUpdateInfosToList(
						clazz,
						ret,
						clazz,
						classUpdateInfos,
						handledOriginalTableNames,
						updateTableNames
				);
			}

			for ( Method method : clazz.getDeclaredMethods() ) {
				UpdateInfo[] methodUpdateInfos = method.getAnnotationsByType( UpdateInfo.class );
				this.addUpdateInfosToList(
						method,
						ret,
						null,
						methodUpdateInfos,
						handledOriginalTableNames,
						updateTableNames
				);
			}

			for ( Field field : clazz.getDeclaredFields() ) {
				UpdateInfo[] fieldUpdateInfos = field.getAnnotationsByType( UpdateInfo.class );
				this.addUpdateInfosToList(
						field,
						ret,
						null,
						fieldUpdateInfos,
						handledOriginalTableNames,
						updateTableNames
				);
			}
		}
		return ret;
	}

	private void addUpdateInfosToList(
			Object specifiedOn,
			List<EventModelInfo> eventModelInfos,
			Class<?> classSpecifiedOnClass,
			UpdateInfo[] infos,
			Set<String> handledOriginalTableNames,
			Set<String> updateTableNames) {
		for ( UpdateInfo info : infos ) {
			String originalTableName = info.tableName();

			if ( updateTableNames.contains( originalTableName ) ) {
				throw new SearchException( specifiedOn + ": naming conflict with table " + originalTableName + ". a table of this name was marked to be created" );
			}

			if ( handledOriginalTableNames.contains( originalTableName ) ) {
				throw new SearchException( specifiedOn + ": multiple @UpdateInfo specified for table " + originalTableName );
			}
			handledOriginalTableNames.add( originalTableName );


			String updateTableName = info.updateTableName().equals( "" ) ?
					originalTableName + "hsearchupdates" :
					info.updateTableName();

			if ( handledOriginalTableNames.contains( updateTableName ) ) {
				throw new SearchException( specifiedOn + ": naming conflict with table " + updateTableName + ". a table of this name was marked to be created" );
			}

			if ( updateTableNames.contains( updateTableName ) ) {
				throw new AssertionFailure( "attempted to use the same UpdateTableName twice: " + updateTableName );
			}

			updateTableNames.add( updateTableName );

			String eventCaseColumn = info.updateTableEventTypeColumn().equals( "" ) ?
					"eventcasehsearch" :
					info.updateTableEventTypeColumn();
			String updateIdColumn = info.updateTableIdColumn().equals( "" ) ?
					"updateidhsearch" :
					info.updateTableIdColumn();

			IdInfo[] annotationIdInfos = info.idInfos();
			List<EventModelInfo.IdInfo> idInfos = new ArrayList<>( annotationIdInfos.length );
			//now handle all the IdInfos
			for ( IdInfo annotationIdInfo : annotationIdInfos ) {
				final Class<?> idInfoEntityClass;
				if ( annotationIdInfo.entity().equals( void.class ) ) {
					if ( classSpecifiedOnClass == null ) {
						throw new SearchException( specifiedOn + ": IdInfo.entity must be specified for the member level!" );
					}
					idInfoEntityClass = classSpecifiedOnClass;
				}
				else {
					idInfoEntityClass = annotationIdInfo.entity();
				}

				boolean foundCustomType = false;

				final String[] columns = new String[annotationIdInfo.columns().length];
				final String[] updateTableColumns = new String[columns.length];
				final ColumnType[] columnTypes = new ColumnType[columns.length];
				final String[] columnDefinitions = new String[columns.length];
				IdColumn[] idColumns = annotationIdInfo.columns();
				for ( int i = 0; i < idColumns.length; ++i ) {
					IdColumn cur = idColumns[i];
					columns[i] = cur.column();
					if ( !"".equals( cur.updateTableColumn() ) ) {
						updateTableColumns[i] = cur.updateTableColumn();
					}
					else {
						updateTableColumns[i] = cur.column() + "fk";
					}
					columnTypes[i] = cur.columnType();
					if ( cur.columnType() == ColumnType.CUSTOM && cur.columnDefinition().equals( "" ) ) {
						throw new SearchException( specifiedOn + ": ColumnType.CUSTOM must be specified together with a columnDefinition" );
					}
					if ( cur.columnType() == ColumnType.CUSTOM ) {
						foundCustomType = true;
					}
					columnDefinitions[i] = cur.columnDefinition();
				}

				final IdConverter idConverter;
				if ( !foundCustomType && IdConverter.class.equals( annotationIdInfo.idConverter() ) && columnTypes.length == 1 ) {
					idConverter = columnTypes[0];
				}
				else {
					if ( IdConverter.class.equals( annotationIdInfo.idConverter() ) ) {
						throw new SearchException(
								specifiedOn + ": if more than one column or a custom columntype is specified, you have to specify an IdConverter"
						);
					}
					try {
						idConverter = annotationIdInfo.idConverter().newInstance();
					}
					catch (InstantiationException | IllegalAccessException e) {
						throw new SearchException( e );
					}
				}

				Map<String, String> hints = new HashMap<>();
				for ( Hint hint : annotationIdInfo.hints() ) {
					hints.put( hint.key(), hint.value() );
				}

				idInfos.add(
						new EventModelInfo.IdInfo(
								idInfoEntityClass,
								updateTableColumns,
								columns,
								columnTypes,
								columnDefinitions,
								idConverter,
								hints
						)
				);
			}

			EventModelInfo evi = new EventModelInfo(
					updateTableName,
					originalTableName,
					eventCaseColumn,
					updateIdColumn,
					idInfos
			);
			eventModelInfos.add( evi );
		}
	}

}
