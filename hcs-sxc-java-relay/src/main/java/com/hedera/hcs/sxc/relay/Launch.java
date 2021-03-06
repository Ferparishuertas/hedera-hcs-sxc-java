package com.hedera.hcs.sxc.relay;

/*-
 * ‌
 * hcs-sxc-java
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hcs.sxc.relay.config.Config;
import com.hedera.hcs.sxc.relay.config.Queue;
import com.hedera.hcs.sxc.relay.subscribe.MirrorTopicSubscriber;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class Launch {

    public static void main(String[] args) throws Exception {
        Config config = new Config();

        String mirrorAddress = config.getConfig().getMirrorAddress();
        String[] mirrorDetails = mirrorAddress.split(":");
        if (mirrorDetails.length != 2) {
            throw new Exception("hcs-sxc-java-relay: mirrorAddress format is incorrect, should be address:port");
        }

        log.debug("Relay topics to subscribe to from mirror and queue");
        boolean blockingSetupJmsTopic = blockingCreateJmsTopic(config);
        log.debug("Queue in relay is set up:" + blockingSetupJmsTopic);
        if (blockingSetupJmsTopic) {
            for (ConsensusTopicId topic : config.getConfig().getTopicIds()) {
                log.debug("Processing topic num: " + topic.topic);
                // subscribe to topic with mirror node
                MirrorTopicSubscriber subscriber = new MirrorTopicSubscriber(
                        mirrorDetails[0]
                        , Integer.parseInt(mirrorDetails[1])
                        , topic
                        , config.getConfig().getCatchupHistory()
                        , config.getConfig().getLastConsensusTimeFile()
                );
                Thread subscriberThread = new Thread(subscriber);
                subscriberThread.start();
            }
        } else {
            log.error("Queue topic subscription error");
            throw new Exception("Queue topic subscription error");
        }
	}

    /**
     * Sets up jms topic.It blocks until the jms queue becomes available.
     * @param config
     * @param topicNum
     * @return true if topic created and communication succeeded.
     * @throws JMSException
     * @throws NamingException
     */
    private static boolean blockingCreateJmsTopic(Config config) throws JMSException, NamingException {
        //JMS config
        Connection connection = null;
        InitialContext initialContext = null;
        boolean r = false;

        try {

            Queue queueConfig = config.getConfig().getQueue();

            Hashtable<String, Object> props = new Hashtable<>();
            props.put(Context.INITIAL_CONTEXT_FACTORY, queueConfig.getInitialContextFactory());

            props.put("topic.topic/hcsTopic", "hcsCatchAllTopics");

            props.put("connectionFactory.TCPConnectionFactory", queueConfig.getTcpConnectionFactory());
            InitialContext ctx = new InitialContext(props);
            ctx.lookup("TCPConnectionFactory");

            initialContext = ctx;

            Topic topic = (Topic) initialContext.lookup("topic/hcsTopic");

            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("TCPConnectionFactory");

            log.debug("Waiting for MQ Artemis to start ...");

            boolean scanning = true;
            do {
                try {
                    connection = cf.createConnection();
                    scanning = false;
                } catch (Exception ie) {
                    String tcpConnectionFactory = queueConfig.getTcpConnectionFactory();
                    log.debug("Is Artemis up? Setup your host file so that the host identified in'"+tcpConnectionFactory+"' points to 127.0.0.1 if running outside of docker");
                    log.debug("Sleeping 6s before retry");
                    TimeUnit.SECONDS.sleep(6);
                }

            } while (scanning);

            connection.setClientID("topic-setup-relay:hcsCatchAllTopics");

            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            session.createProducer(topic);

            r = true;

        } catch (Exception e) {
            log.error(e);
        } finally {
            if (connection != null) {
                log.debug("blockingCreateJmsTopic - Closing JMS connection");
                connection.close();
            }
            if (initialContext != null) {
                log.debug("blockingCreateJmsTopic- Closing JMS initial context");
                initialContext.close();
            }
        }
         return r;
    }

}
