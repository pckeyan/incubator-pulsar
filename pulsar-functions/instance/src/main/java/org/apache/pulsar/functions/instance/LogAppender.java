/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.functions.instance;

import org.apache.logging.log4j.core.*;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * LogAppender class that is used to send log statements from Pulsar Functions logger
 * to a log topic.
 */
public class LogAppender implements Appender {
    private PulsarClient pulsarClient;
    private String logTopic;
    private String fqn;
    private State state;
    private ErrorHandler errorHandler;
    private Producer<byte[]> producer;

    public LogAppender(PulsarClient pulsarClient, String logTopic, String fqn) {
        this.pulsarClient = pulsarClient;
        this.logTopic = logTopic;
        this.fqn = fqn;
    }

    @Override
    public void append(LogEvent logEvent) {
        producer.sendAsync(logEvent.getMessage().getFormattedMessage().getBytes());
    }

    @Override
    public String getName() {
        return fqn;
    }

    @Override
    public Layout<? extends Serializable> getLayout() {
        return null;
    }

    @Override
    public boolean ignoreExceptions() {
        return false;
    }

    @Override
    public ErrorHandler getHandler() {
        return errorHandler;
    }

    @Override
    public void setHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void initialize() {
        this.state = State.INITIALIZED;
    }

    @Override
    public void start() {
        this.state = State.STARTING;
        try {
            ProducerBuilder<byte[]> producerBuilder = pulsarClient.newProducer()
                    .topic(logTopic)
                    .producerName(fqn)
                    .blockIfQueueFull(false)
                    .enableBatching(true)
                    .batchingMaxPublishDelay(10, TimeUnit.MILLISECONDS);
            producer = producerBuilder.create();
        } catch (Exception e) {
            throw new RuntimeException("Error starting LogTopic Producer", e);
        }
        this.state = State.STARTED;
    }

    @Override
    public void stop() {
        this.state = State.STOPPING;
        producer.closeAsync();
        producer = null;
        this.state = State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
