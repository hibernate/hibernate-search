/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.IdConverter;
import org.hibernate.search.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.events.impl.EventModelParser;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * This EventModelParser should be used when Hibernate ORM
 * is available we have more information about how stuff
 * is persisted available compared to plain JPA
 *
 * @author Martin Braun
 */
public class ORMEventModelParser implements EventModelParser {

	public static final String HSEARCH_UPDATES_SUFFIX = "hsearchupdates";
	public static final String EVENT_TYPE_COLUMN = HSEARCH_UPDATES_SUFFIX + "eventType" + HSEARCH_UPDATES_SUFFIX;
	public static final String UPDATE_ID_COLUMN = "hs" + "updateId" + HSEARCH_UPDATES_SUFFIX;

	private static final Log log = LoggerFactory.make( Log.class );

	private final EventModelParser manualParser;
	private final SessionFactory sessionFactory;
	private final Set<Class<?>> indexRelevantEntities;

	public ORMEventModelParser(SessionFactory sessionFactory, Set<Class<?>> indexRelevantEntities) {
		this.sessionFactory = sessionFactory;
		this.indexRelevantEntities = indexRelevantEntities;
		this.manualParser = new AnnotationEventModelParser();
	}

	/**
	 * used for tests
	 *
	 * @param sessionFactory
	 * @param manualParser
	 */
	public ORMEventModelParser(
			SessionFactory sessionFactory,
			Set<Class<?>> indexRelevantEntities,
			EventModelParser manualParser) {
		this.sessionFactory = sessionFactory;
		this.indexRelevantEntities = indexRelevantEntities;
		this.manualParser = manualParser;
	}

	@Override
	public List<EventModelInfo> parse(List<Class<?>> updateClasses) {
		SessionFactoryImplementor impl = (SessionFactoryImplementor) this.sessionFactory;

		Map<String, EventModelInfo> tableEventModelInfos = new HashMap<>();
		Map<String, Set<String>> tableHandledColumns = new HashMap<>();

		try {
			Method getTableSpan = AbstractEntityPersister.class.getDeclaredMethod( "getTableSpan" );
			getTableSpan.setAccessible( true );
			Method getTableName = AbstractEntityPersister.class.getDeclaredMethod( "getTableName", int.class );
			getTableName.setAccessible( true );
			Method getKeyColumns = AbstractEntityPersister.class.getDeclaredMethod( "getKeyColumns", int.class );
			getKeyColumns.setAccessible( true );

			for ( Class<?> updateClass : updateClasses ) {
				if ( !this.indexRelevantEntities.contains( updateClass ) ) {
					continue;
				}

				ClassMetadata metadata = this.sessionFactory.getClassMetadata( updateClass );
				if ( !(metadata instanceof AbstractEntityPersister) ) {
					//attempt to get the info from @UpdateInfo annotations
					//these get added automatically
					log.usingAnnotationParserForTypeNoAEPFound( updateClass );
				}
				else {
					AbstractEntityPersister entityPersister = (AbstractEntityPersister) this.sessionFactory.getClassMetadata(
							updateClass
					);

					//TODO: is the relation table detection okay?

					//FIXME: check subclasses
					int tableSpan = (Integer) getTableSpan.invoke( entityPersister );
					for ( int i = 0; i < tableSpan; ++i ) {
						String originalTableName = (String) getTableName.invoke( entityPersister, i );

						if ( tableEventModelInfos.containsKey( originalTableName ) ) {
							//TODO: is this okay?
							//this is an association table and was already handled
							continue;
						}

						List<EventModelInfo.IdInfo> idInfos = this.getIdInfoList(
								tableEventModelInfos,
								originalTableName
						);

						String[] keyColumns = (String[]) getKeyColumns.invoke( entityPersister, i );
						this.addIdInfo(
								keyColumns,
								updateClass,
								entityPersister.getIdentifierType().getReturnedClass(),
								idInfos, originalTableName, tableHandledColumns
						);
					}

					//Associations (only one side is needed if @ContainedIn is used and
					//every entity has a property pointing back to the owning side)
					for ( String property : entityPersister.getPropertyNames() ) {
						Type type = entityPersister.getPropertyType( property );
						if ( type.isAssociationType() ) {
							AssociationType associationType = (AssociationType) type;
							Joinable joinable = associationType.getAssociatedJoinable( impl );
							String[] keyColumns = joinable.getKeyColumnNames();

							//TODO: is this correct?
							//if this is an association there could have been a owning side
							//entity that had some Entity handled badly in the code above.
							//we fix this here by deleting it. as long as one is correct here
							//we should be fine
							tableHandledColumns.remove( joinable.getTableName() );
							tableEventModelInfos.remove( joinable.getTableName() );

							String entityName;
							if ( associationType instanceof CollectionType ) {
								CollectionType collectionType = ((CollectionType) associationType);
								QueryableCollection collectionPersister = (QueryableCollection) impl
										.getCollectionPersister( collectionType.getRole() );

								//this code is from CollectionType. an Exception would be raised
								//if we didn't do these check and used getAssociatedEntityName
								if ( !collectionPersister.getElementType().isEntityType() ) {
									//this is something like an EmbeddedCollection
									//so the only identifier there is is
									//the one of the actual entity
									entityName = entityPersister.getEntityName();
								}
								else {
									//the other side is a real Entity
									entityName = associationType.getAssociatedEntityName( impl );
								}
							}
							else {
								//the other side is a real Entity
								entityName = associationType.getAssociatedEntityName( impl );
							}

							AbstractEntityPersister persisterForOther = (AbstractEntityPersister) impl.getClassMetadata(
									entityName
							);
							Class<?> associationEntityClass = persisterForOther.getMappedClass();

							//this is not needed
							if ( !this.indexRelevantEntities.contains( associationEntityClass ) ) {
								continue;
							}

							List<EventModelInfo.IdInfo> idInfos = this.getIdInfoList(
									tableEventModelInfos,
									joinable.getTableName()
							);

							this.addIdInfo(
									keyColumns,
									associationEntityClass,
									persisterForOther.getIdentifierType().getReturnedClass(),
									idInfos, joinable.getTableName(), tableHandledColumns
							);
						}
					}
				}
			}


			List<EventModelInfo> manuallySetValues = this.manualParser.parse( updateClasses );
			Map<String, EventModelInfo> manuallySetPerOriginal = new HashMap<>();
			Map<String, EventModelInfo> manuallySetPerUpdate = new HashMap<>();
			for ( EventModelInfo eventModelInfo : manuallySetValues ) {
				manuallySetPerOriginal.put( eventModelInfo.getOriginalTableName(), eventModelInfo );
				manuallySetPerUpdate.put( eventModelInfo.getUpdateTableName(), eventModelInfo );
			}

			List<EventModelInfo> ret = new ArrayList<>( tableEventModelInfos.size() + manuallySetValues.size() );
			ret.addAll( manuallySetValues );

			for ( Map.Entry<String, EventModelInfo> entry : tableEventModelInfos.entrySet() ) {
				String tableName = entry.getKey();
				EventModelInfo evi = entry.getValue();
				if ( !manuallySetPerOriginal.containsKey( tableName ) ) {
					//this was not already specified via annotations so we include it
					if ( manuallySetPerUpdate.containsKey( evi.getUpdateTableName() ) ) {
						EventModelInfo conflicting = manuallySetPerUpdate.get( evi.getUpdateTableName() );
						throw log.namingConflictEventModel(
								conflicting.getOriginalTableName(),
								conflicting.getUpdateTableName()
						);
					}
					ret.add( evi );
				}
			}
			return ret;
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new AssertionFailure( "unexpected Exception occured", e );
		}
	}

	private List<EventModelInfo.IdInfo> getIdInfoList(
			Map<String, EventModelInfo> tableEventModelInfos,
			String originalTableName) {
		//find out if we have already built up some information for this
		EventModelInfo eventModelInfo = tableEventModelInfos.get( originalTableName );

		List<EventModelInfo.IdInfo> idInfos;
		if ( eventModelInfo == null ) {
			String updateTableName = originalTableName + HSEARCH_UPDATES_SUFFIX;
			String eventTypeColumn = EVENT_TYPE_COLUMN;
			String updateIdColumn = UPDATE_ID_COLUMN;
			idInfos = new ArrayList<>();
			eventModelInfo = new EventModelInfo(
					updateTableName,
					originalTableName,
					eventTypeColumn,
					updateIdColumn,
					idInfos
			);
			//put it in our memory map
			tableEventModelInfos.put( originalTableName, eventModelInfo );
		}
		return eventModelInfo.getIdInfos();
	}

	private void addIdInfo(
			String[] keyColumns,
			Class<?> updateClass,
			Class<?> identifierClass,
			List<EventModelInfo.IdInfo> idInfos,
			String originalTableName,
			Map<String, Set<String>> tableHandledColumns) {
		{
			//hacky
			if ( keyColumns.length != 1 ) {
				throw new AssertionFailure( "can't handle ids with more than one column, yet" );
			}

			String keyColumn = keyColumns[0];
			if ( tableHandledColumns.computeIfAbsent( originalTableName, (key) -> new HashSet<>() )
					.contains( keyColumn ) ) {
				return;
			}
			tableHandledColumns.get( originalTableName ).add( keyColumn );

			Class<?> entityClass = updateClass;
			String[] columnsInUpdateTable = new String[] {keyColumn + HSEARCH_UPDATES_SUFFIX};
			String[] columnsInOriginal = new String[] {keyColumn};
			ColumnType[] columnTypes;
			if ( identifierClass.equals( String.class ) ) {
				columnTypes = new ColumnType[] {ColumnType.STRING};
			}
			else if ( identifierClass.equals( Integer.class ) ) {
				columnTypes = new ColumnType[] {ColumnType.INTEGER};
			}
			else if ( identifierClass.equals( Long.class ) ) {
				columnTypes = new ColumnType[] {ColumnType.LONG};
			}
			else {
				columnTypes = new ColumnType[] {ColumnType.CUSTOM};
				throw new AssertionFailure( "only String, Integer, Long allowed for ids" );
			}
			String[] columnDefinitions = new String[] {""};
			IdConverter idConverter = columnTypes[0];
			Map<String, String> hints = new HashMap<>();

			idInfos.add(
					new EventModelInfo.IdInfo(
							entityClass,
							columnsInUpdateTable,
							columnsInOriginal,
							columnTypes,
							columnDefinitions,
							idConverter, hints
					)
			);
		}
	}

}
