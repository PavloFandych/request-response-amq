package org.total.amq.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.total.amq.protocol.MessageProtocol;

import javax.jms.*;

/**
 * @author Pavlo.Fandych
 */

@Slf4j
public class Server {

    private Session session;

    private MessageProducer responseProducer;

    private MessageProtocol messageProtocol = new MessageProtocol();

    private Server() {
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            final Destination clientRequest = session.createQueue("client.messages");

            responseProducer = session.createProducer(null);

            final MessageConsumer consumer = session.createConsumer(clientRequest);
            consumer.setMessageListener(message -> {
                try {
                    final TextMessage response = session.createTextMessage();
                    if (message instanceof TextMessage) {
                        final String messageText = ((TextMessage) message).getText();
                        response.setText(messageProtocol.handleProtocolMessage(messageText));
                    }

                    response.setJMSCorrelationID(message.getJMSCorrelationID());

                    responseProducer.send(message.getJMSReplyTo(), response);
                } catch (JMSException e) {
                    log.info("Exception occurred", e);
                }
            });
        } catch (JMSException e) {
            log.info("Exception occurred", e);
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
