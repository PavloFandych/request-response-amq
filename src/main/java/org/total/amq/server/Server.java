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

    private static final String EXCEPTION_OCCURRED = "Exception occurred";

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
            consumer.setMessageListener(request -> {
                try {
                    final TextMessage response = session.createTextMessage();
                    if (request instanceof TextMessage) {
                        final String messageText = ((TextMessage) request).getText();
                        response.setText(messageProtocol.handleProtocolMessage(messageText));
                    }

                    response.setJMSMessageID(request.getJMSMessageID());
                    response.setJMSCorrelationID(request.getJMSCorrelationID());

                    responseProducer.send(request.getJMSReplyTo(), response);
                } catch (JMSException e) {
                    log.info(EXCEPTION_OCCURRED, e);
                }
            });
        } catch (JMSException e) {
            log.info(EXCEPTION_OCCURRED, e);
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
