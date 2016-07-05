package org.hibernate.search.jsr352.internal;

import java.util.Properties;
import java.util.Set;

import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class EntityPartitionMapper implements PartitionMapper {

	private static final Logger logger = Logger.getLogger(EntityPartitionMapper.class);

	private final JobContext jobContext;

	@Inject
    public EntityPartitionMapper(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
    public PartitionPlan mapPartitions() throws Exception {
        Set<Class<?>> rootEntities = ( (BatchContextData) jobContext.getTransientUserData() ).getEntityTypesToIndex();

        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                logger.infof("%d partitions.", rootEntities.size() );
                return rootEntities.size();
            }

            @Override
            public int getThreads() {
                logger.infof("%d threads.", getPartitions());
                return getPartitions();
            }

			@Override
			public Properties[] getPartitionProperties() {
				Properties[] props = new Properties[getPartitions()];
				int i = 0;
				for ( Class<?> entityType : rootEntities ) {
					props[i] = new Properties();
					props[i].setProperty( "entityType", entityType.getName() );
					i++;
				}

				return props;
			}
        };
    }
}
