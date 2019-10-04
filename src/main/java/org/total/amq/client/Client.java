package org.total.amq.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.UUID;

/**
 * @author Pavlo.Fandych
 */

@Slf4j
public class Client {

    private static final String EXCEPTION_OCCURRED = "Exception occurred";

    private Client() {
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (final Connection connection = connectionFactory.createConnection();
             final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();

            final Destination clientRequest = session.createQueue("client.messages");
            final Destination temporaryQueue = session.createTemporaryQueue();

            final MessageConsumer responseConsumer = session.createConsumer(temporaryQueue);
            responseConsumer.setMessageListener(localMessage -> {
                try {
                    if (localMessage instanceof TextMessage) {
                        final String messageText = ((TextMessage) localMessage).getText();

                        log.info("messageText from AMQ =======> " + messageText);
                    }
                } catch (JMSException e) {
                    log.info(EXCEPTION_OCCURRED, e);
                }
            });

            final TextMessage message = generateRequest(session, temporaryQueue);

            sendMessage(session, clientRequest, message);
        } catch (JMSException e) {
            log.info(EXCEPTION_OCCURRED, e);
        }
    }

    public static void main(String[] args) {
        new Client();
    }

    private TextMessage generateRequest(final Session session,
                                        final Destination temporaryQueue) throws JMSException {
        final TextMessage request = session.createTextMessage();
        request.setText("MyProtocolMessage");
        request.setJMSReplyTo(temporaryQueue);

        final String id = UUID.randomUUID().toString();
        request.setJMSCorrelationID(id);
        request.setJMSMessageID(id);

        return request;
    }

    private void sendMessage(final Session session,
                             final Destination clientRequest,
                             final TextMessage message) {
        try (final MessageProducer producer = session.createProducer(clientRequest);) {
            producer.send(message);
        } catch (Exception e) {
            log.info(EXCEPTION_OCCURRED, e);
        }
    }
}