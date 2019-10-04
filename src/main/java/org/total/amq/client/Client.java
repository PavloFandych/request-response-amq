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

    private Client() {
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (final Connection connection = connectionFactory.createConnection();
                final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);) {
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
                    log.info("Exception occurred", e);
                }
            });

            final TextMessage message = generateRequest(session, temporaryQueue);

            sendMessage(session, clientRequest, message);
        } catch (JMSException e) {
            log.info("Exception occurred", e);
        }
    }

    public static void main(String[] args) {
        new Client();
    }

    private TextMessage generateRequest(Session session, Destination temporaryQueue) throws JMSException {
        final TextMessage message = session.createTextMessage();
        message.setText("MyProtocolMessage");
        message.setJMSReplyTo(temporaryQueue);
        message.setJMSCorrelationID(UUID.randomUUID().toString());

        return message;
    }

    private void sendMessage(final Session session, final Destination clientRequest, final TextMessage message) {
        try (final MessageProducer producer = session.createProducer(clientRequest);) {
            producer.send(message);
        } catch (Exception e) {
            log.info("Exception occurred", e);
        }
    }
}