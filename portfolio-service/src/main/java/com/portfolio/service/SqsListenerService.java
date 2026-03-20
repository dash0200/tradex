package com.portfolio.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.model.TradeMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SqsListenerService {

    private final AmazonSQS sqsClient;
    private final PortfolioRepository portfolioRepository;
    private final ObjectMapper objectMapper;
    private String queueUrl;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    public SqsListenerService(PortfolioRepository portfolioRepository) {
        this.sqsClient = AmazonSQSClientBuilder.standard().build();
        this.portfolioRepository = portfolioRepository;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        this.queueUrl = sqsClient.getQueueUrl(queueName).getQueueUrl();
        System.out.println("Initialized SQS Listener for queue: " + queueUrl);
    }

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void pollQueue() {
        ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest(queueUrl)
                .withMaxNumberOfMessages(10)
                .withWaitTimeSeconds(5);

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();

        for (Message m : messages) {
            try {
                // SNS wraps the message inside its own JSON envelope payload. The actual message is in the 'Message' field.
                JsonNode snsNode = objectMapper.readTree(m.getBody());
                String innerMessage = snsNode.has("Message") ? snsNode.get("Message").asText() : m.getBody();
                
                TradeMessage trade = objectMapper.readValue(innerMessage, TradeMessage.class);

                int qtyChange = "BUY".equals(trade.getTradeType()) ? trade.getQuantity() : -trade.getQuantity();
                double valueChange = "BUY".equals(trade.getTradeType()) ? (trade.getQuantity() * trade.getPrice()) : -(trade.getQuantity() * trade.getPrice());

                portfolioRepository.updateHolding(trade.getUserId(), trade.getPortfolioId(), trade.getSymbol(), qtyChange, valueChange);

                sqsClient.deleteMessage(new DeleteMessageRequest(queueUrl, m.getReceiptHandle()));

            } catch (Exception e) {
                System.err.println("Failed to process message: " + m.getBody());
                e.printStackTrace();
            }
        }
    }
}
