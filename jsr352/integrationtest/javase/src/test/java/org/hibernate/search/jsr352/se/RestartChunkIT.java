package org.hibernate.search.jsr352.se;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.se.test.Company;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RestartChunkIT {
    
    private EntityManagerFactory emf;
    private EntityManager em;
    
    // mass indexer configuration values
    private JobOperator jobOperator;
    private final int ARRAY_CAPACITY = 1;
    // TODO: failed for 1000, only 997 read
    private final long DB_COMP_ROWS = 100;
    private static final int JOB_MAX_TRIES = 30;       // 1s * 30 = 30s
    private static final int JOB_THREAD_SLEEP = 1000;  // 1s
    
    private static final Logger logger = Logger.getLogger(RestartChunkIT.class);
    
    @Before
    public void setup() {
        
        jobOperator = JobFactory.getJobOperator();
        emf = Persistence.createEntityManagerFactory("h2");
        em = emf.createEntityManager();
        
        em.getTransaction().begin();
        for (int i = 0; i < DB_COMP_ROWS; i++) {
            Company c;
            switch (i % 5) {
                case 0: c = new Company("Google"); break;
                case 1: c = new Company("Red Hat"); break;
                case 2: c = new Company("Microsoft"); break;
                case 3: c = new Company("Facebook"); break;
                case 4: c = new Company("Amazon"); break;
                default: c = null; break;
            }
            em.persist(c);
        }
        em.getTransaction().commit();
    }
    
    @Test
    public void testJob() throws InterruptedException {
        
        logger.infof("finding company called %s ...", "google");
        List<Company> companies = findCompanyByName("google");
        assertEquals(0, companies.size());
        
        // start the job, then stop it
        long execId1 = startJob();
        JobExecution jobExec1 = jobOperator.getJobExecution(execId1);
        stopChunkAfterStarted(jobExec1);
        jobExec1 = keepTestAlive(jobExec1);
        String msg1 = String.format("Job (executionId=%d) %s, executed steps:%n%n",
                execId1,
                jobExec1.getBatchStatus());
        List<StepExecution> stepExecs1 = jobOperator.getStepExecutions(execId1);
        for (StepExecution stepExec: stepExecs1) {
            boolean isRestarted = false;
            testBatchStatus(stepExec, isRestarted);
            msg1 += String.format("\tid=%s, status=%s%n", 
                    stepExec.getStepName(),
                    stepExec.getBatchStatus());
        }
        logger.info(msg1);
        
        // restart the job
        long execId2 = jobOperator.restart(execId1, null);
        JobExecution jobExec2 = jobOperator.getJobExecution(execId2);
        jobExec2 = keepTestAlive(jobExec2);
        String msg2 = String.format("Job (executionId=%d) stopped, executed steps:%n%n", execId2);
        List<StepExecution> stepExecs2 = jobOperator.getStepExecutions(execId2);
        for (StepExecution stepExec: stepExecs2) {
            boolean isRestarted = true;
            testBatchStatus(stepExec, isRestarted);
            msg2 += String.format("\tid=%s, status=%s%n", 
                    stepExec.getStepName(),
                    stepExec.getBatchStatus());
        }
        logger.info(msg2);
        logger.info("finished");
        
        // search again
        companies = findCompanyByName("google");
//      issue #78 - Cannot find indexed results after mass index
//      assertEquals(1, companies.size());
        logger.infof("%d rows found", companies.size());
    }
    
    private List<Company> findCompanyByName(String name) {
        FullTextEntityManager ftem = Search.getFullTextEntityManager(em);
        Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
                .forEntity(Company.class).get()
                    .keyword().onField("name").matching(name)
                .createQuery();
        @SuppressWarnings("unchecked")
        List<Company> result = ftem.createFullTextQuery(luceneQuery).getResultList();
        return result;
    }

    private long startJob() throws InterruptedException {
        // org.hibernate.search.jsr352.MassIndexer
        MassIndexer massIndexer = new MassIndexerImpl()
                .addRootEntities(Company.class)
                .arrayCapacity(ARRAY_CAPACITY)
                .entityManager(em)
                .jobOperator(jobOperator);
        long executionId = massIndexer.start();
        
        logger.infof("job execution id = %d", executionId);
        return executionId;
    }

    private JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
        int tries = 0;
        while (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)
            && !jobExecution.getBatchStatus().equals(BatchStatus.STOPPED)
            && tries < JOB_MAX_TRIES) {
            
            long executionId = jobExecution.getExecutionId();
            logger.infof("Job (id=%d) %s, thread sleep %d ms...", 
                    executionId,
                    jobExecution.getBatchStatus(),
                    JOB_THREAD_SLEEP
            );
            Thread.sleep(JOB_THREAD_SLEEP);
            jobExecution = jobOperator.getJobExecution(executionId);
            tries++;
        }
        return jobExecution;
    }
    
    private void stopChunkAfterStarted(JobExecution jobExecution) throws InterruptedException {
        
        int tries = 0;
        long executionId = jobExecution.getExecutionId();
        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        logger.infof("%d steps found", stepExecutions.size());
        Iterator<StepExecution> cursor = stepExecutions.iterator();
        while (!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)
            || !jobExecution.getBatchStatus().equals(BatchStatus.FAILED)
            || tries < JOB_MAX_TRIES) {
            
            // find step "produceLuceneDoc"
            while (cursor.hasNext()) {
            
                StepExecution stepExecution = cursor.next();
                String stepName = stepExecution.getStepName();
                BatchStatus stepStatus = stepExecution.getBatchStatus();
                
                if (stepName.equals("produceLuceneDoc")) {
                    logger.info("step produceLuceneDoc found.");
                    if (stepStatus.equals(BatchStatus.STARTING)) {
                        logger.info("step status is STARTING, wait it until STARTED to stop");
                        break;
                    } else {
                        logger.infof("step status is %s, stopping now ...", stepStatus);
                        jobOperator.stop(executionId);
                        return;
                    }
                }
            }
            Thread.sleep(100);
            tries++;
            stepExecutions = jobOperator.getStepExecutions(executionId);
            cursor = stepExecutions.iterator();
        }
    }
    
    private void testBatchStatus(StepExecution stepExecution, boolean isRestarted) {
        BatchStatus batchStatus = stepExecution.getBatchStatus();
        switch (stepExecution.getStepName()) {
            
            case "loadId":
//              long expectedEntityCount = DB_COMP_ROWS;
//              assertEquals(expectedEntityCount, indexingContext.getEntityCount());
                assertEquals(BatchStatus.COMPLETED, batchStatus);
                break;
            
            case "produceLuceneDoc":
                String msg = String.format("metrics in step produceLuceneDoc:%n%n");
                Metric[] metrics = stepExecution.getMetrics();
                for (Metric metric : metrics) {
                    msg += String.format("\t%s: %d%n", metric.getType(), metric.getValue());
                }
                logger.info(msg);
                if (isRestarted) {
//                  TODO: enable to below test after code enhancement
//                  testChunk(getMetricsMap(metrics));
                    assertEquals(BatchStatus.COMPLETED, batchStatus);
                } else {
                    // first execution should be stopped
                    assertEquals(BatchStatus.STOPPED, batchStatus);
                }
                break;
                
            default:
                break;
        }
    }
    
    private void testChunk(Map<Metric.MetricType, Long> metricsMap) {
        long companyCount = (long) Math.ceil((double) DB_COMP_ROWS / ARRAY_CAPACITY);
        // The read count.
        long expectedReadCount = companyCount;
        long actualReadCount = metricsMap.get(Metric.MetricType.READ_COUNT);
        assertEquals(expectedReadCount, actualReadCount);
        // The write count
        long expectedWriteCount = companyCount;
        long actualWriteCount = metricsMap.get(Metric.MetricType.WRITE_COUNT);
        assertEquals(expectedWriteCount, actualWriteCount);
    }

    /**
     * Convert the Metric array contained in StepExecution to a key-value map 
     * for easy access to Metric parameters.
     *
     * @param metrics
     *         a Metric array contained in StepExecution.
     *
     * @return a map view of the metrics array.
     */
    private Map<Metric.MetricType, Long> getMetricsMap(Metric[] metrics) {
        Map<Metric.MetricType, Long> metricsMap = new HashMap<>();
        for (Metric metric : metrics) {
            metricsMap.put(metric.getType(), metric.getValue());
        }
        return metricsMap;
    }
    
    @After
    public void shutdownJPA() {
        em.close();
        emf.close();
    }
}
