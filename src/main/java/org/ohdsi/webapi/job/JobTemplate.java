package org.ohdsi.webapi.job;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

/**
 *
 */
public class JobTemplate {
    
    private static final Log log = LogFactory.getLog(JobTemplate.class);
    
    private final JobLauncher jobLauncher;
    
    public JobTemplate(final JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }
    
    public JobExecutionResource launch(final Job job, final JobParameters jobParameters) throws WebApplicationException {
        JobExecution exec = null;
        try {
            exec = this.jobLauncher.run(job, jobParameters);
            if (log.isDebugEnabled()) {
                log.debug("JobExecution queued: " + exec);
            }
        } catch (final JobExecutionAlreadyRunningException e) {
            throw new WebApplicationException(Response.status(Status.CONFLICT).entity(e.getMessage()).build());
        } catch (final Exception e) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        return JobUtils.toJobExecutionResource(exec);
    }
    
}