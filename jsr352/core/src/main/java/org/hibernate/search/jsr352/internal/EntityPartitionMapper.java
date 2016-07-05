package org.hibernate.search.jsr352.internal;

import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class EntityPartitionMapper implements PartitionMapper {

    @Inject
    private IndexingContext indexingContext;
    
    @Inject @BatchProperty(name = "rootEntities")
    private String rootEntitiesStr;
    
    private static final Logger logger = Logger.getLogger(EntityPartitionMapper.class);
    
    @Override
    public PartitionPlan mapPartitions() throws Exception {
        
//      String[] rootEntities = parse(rootEntitiesStr);
        Class<?>[] rootEntities = indexingContext.getRootEntities();
        
        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                logger.infof("%d partitions.", rootEntities.length);
                return rootEntities.length;
            }

            @Override
            public int getThreads() {
                logger.infof("%d threads.", getPartitions());
                return getPartitions();
            }

            @Override
            public Properties[] getPartitionProperties() {
                Properties[] props = new Properties[getPartitions()];
                for (int i = 0; i < props.length; i++) {
                    props[i] = new Properties();
                    props[i].setProperty("entityType", rootEntities[i].getName());
                }
                return props;
            }
        };
    }
    
    /**
     * Parse a set of entities in string into a set of entity-types.
     * 
     * @param raw a set of entities concatenated in string, separated by ","
     *          and surrounded by "[]", e.g. "[com.xx.foo, com.xx.bar]".
     * @return a set of entity-types
     * @throws NullPointerException thrown if the entity-token is not found.
     */
    private String[] parse(String raw) throws NullPointerException {
        if (raw == null) {
            throw new NullPointerException("Not any target entity to index");
        }
        String[] rootEntities = raw
                .substring(1, raw.length() - 1)
                .split(", ");
        return rootEntities;
    }
}
