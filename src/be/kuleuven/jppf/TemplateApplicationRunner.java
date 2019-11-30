/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.kuleuven.jppf;

import java.util.ArrayList;
import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.Operator;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class TemplateApplicationRunner {

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   */
  public static void main(final String...args) {

    // create the JPPFClient. This constructor call causes JPPF to read the configuration file
    // and connect with one or multiple JPPF drivers.
    try (final JPPFClient jppfClient = new JPPFClient()) {

      // create a runner instance.
      final TemplateApplicationRunner runner = new TemplateApplicationRunner();

      // create and execute a blocking job
//      runner.executeBlockingJob(jppfClient);

      // create and execute a non-blocking job
      runner.executeNonBlockingJob(jppfClient);

    } catch(final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a JPPF job that can be submitted for execution.
   * @param jobName an arbitrary, human-readable name given to the job.
   * @return an instance of the {@link JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public JPPFJob createJob(final String jobName) throws Exception {
    // create a JPPF job
    final JPPFJob job = new JPPFJob();
    // give this job a readable name that we can use to monitor and manage it.
    job.setName(jobName);

    // add a task to the job.
    final Task<?> task = job.add(new TemplateJPPFTask());
    // provide a user-defined name for the task
    task.setId(jobName + " - Template task");

    // add more tasks here ...

    // there is no guarantee on the order of execution of the tasks,
    // however the results are guaranteed to be returned in the same order as the tasks.
    return job;
  }

  /**
   * Execute a job in blocking mode. The application will be blocked until the job execution is complete.
   * @param jppfClient the {@link JPPFClient} instance which submits the job for execution.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeBlockingJob(final JPPFClient jppfClient) throws Exception {
    // Create a job
    final JPPFJob job = createJob("Template blocking job");

    // Submit the job and wait until the results are returned.
    // The results are returned as a list of Task<?> instances,
    // in the same order as the one in which the tasks where initially added to the job.
    final List<Task<?>> results = jppfClient.submit(job);

    // process the results
    processExecutionResults(job.getName(), results);
  }

  /**
   * Execute a job in non-blocking mode. The application has the responsibility
   * for handling the notification of job completion and collecting the results.
   * @param jppfClient the {@link JPPFClient} instance which submits the job for execution.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeNonBlockingJob(final JPPFClient jppfClient) throws Exception {
    // Create a job
    final JPPFJob job = createJob("Template non-blocking job");

    // Submit the job. This call returns immediately without waiting for the execution of
    // the job to complete. As a consequence, the object returned for a non-blocking job is
    // always null. Note that we are calling the exact same method as in the blocking case.
    jppfClient.submitAsync(job);

    // the non-blocking job execution is asynchronous, we can do anything else in the meantime
    System.out.println("Doing something while the job is executing ...");
    // ...

    // We are now ready to get the results of the job execution.
    // We use JPPFJob.awaitResults() for this. This method returns immediately with
    // the results if the job has completed, otherwise it waits until the job execution is complete.
    final List<Task<?>> results = job.awaitResults();

    // process the results
    processExecutionResults(job.getName(), results);
  }

  /**
   * Process the execution results of each submitted task.
   * @param jobName the name of the job whose results are processed. 
   * @param results the tasks results after execution on the grid.
   */
  public synchronized void processExecutionResults(final String jobName, final List<Task<?>> results) {
    // print a results header
    System.out.printf("Results for job '%s' :\n", jobName);
    // process the results
    for (final Task<?> task: results) {
      final String taskName = task.getId();
      // if the task execution resulted in an exception
      if (task.getThrowable() != null) {
        // process the exception here ...
        System.out.println(taskName + ", an exception was raised: " + task.getThrowable ().getMessage());
      } else {
        // process the result here ...
        System.out.println(taskName + ", execution result: " + task.getResult());
      }
    }
  }
}
