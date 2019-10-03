package org.total.amq.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.UUID;

/**
 * @author Pavlo.Fandych
 */

@Slf4j
public class Client implements MessageListener {

    private MessageProducer producer;

    private Client() {
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();

            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final Destination clientRequest = session.createQueue("client.messages");
            final Destination temporaryQueue = session.createTemporaryQueue();

            final MessageConsumer responseConsumer = session.createConsumer(temporaryQueue);
            responseConsumer.setMessageListener(this);

            final TextMessage message = session.createTextMessage();
            message.setText("MyProtocolMessage");
            message.setJMSReplyTo(temporaryQueue);
            message.setJMSCorrelationID(UUID.randomUUID().toString());

            producer = session.createProducer(clientRequest);
            producer.send(message);
        } catch (JMSException e) {
            log.info("Exception occurred", e);
        }
    }

    public static void main(String[] args) {
        new Client();
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                final String messageText = ((TextMessage) message).getText();

                log.info("messageText from AMQ =======> " + messageText);
            }
        } catch (JMSException e) {
            log.info("Exception occurred", e);
        }
    }
}