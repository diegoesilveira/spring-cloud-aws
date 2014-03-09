/*
 * Copyright 2013 the original author or authors.
 *
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
 */

package org.elasticspring.messaging.core;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueMessageChannelTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void sendMessage_validTextMessage_returnsTrue() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		Mockito.verify(amazonSqs, Mockito.only()).
				sendMessage(new SendMessageRequest("http://testQueue", "message content").withDelaySeconds(0));
		Assert.assertTrue(sent);
	}

	@Test
	public void sendMessage_serviceThrowsError_throwsMessagingException() throws Exception {
		//Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");
		Mockito.when(amazonSqs.sendMessage(new SendMessageRequest("http://testQueue", "message content").withDelaySeconds(0))).
				thenThrow(new AmazonServiceException("wanted error"));

		//Assert
		this.expectedException.expect(MessagingException.class);
		this.expectedException.expectMessage("wanted error");

		//Act
		messageChannel.send(stringMessage);
	}

	@Test
	public void receiveMessage_withoutTimeout_returnsTextMessage() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		Mockito.when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(0).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.singleton(new com.amazonaws.services.sqs.model.Message().withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive();

		//Assert
		Assert.assertNotNull(receivedMessage);
		Assert.assertEquals("content", receivedMessage.getPayload());
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsTextMessage() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		Mockito.when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(2).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.singleton(new com.amazonaws.services.sqs.model.Message().withBody("content"))));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(2);

		//Assert
		Assert.assertNotNull(receivedMessage);
		Assert.assertEquals("content", receivedMessage.getPayload());
	}

	@Test
	public void receiveMessage_withSpecifiedTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		Mockito.when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(2).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.<com.amazonaws.services.sqs.model.Message>emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(2);

		//Assert
		Assert.assertNull(receivedMessage);
	}

	@Test
	public void receiveMessage_withoutDefaultTimeout_returnsNull() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		Mockito.when(amazonSqs.receiveMessage(new ReceiveMessageRequest("http://testQueue").
				withWaitTimeSeconds(0).
				withMaxNumberOfMessages(1).
				withAttributeNames(QueueMessageChannel.MESSAGE_RECEIVING_ATTRIBUTE_NAMES))).
				thenReturn(new ReceiveMessageResult().withMessages(
						Collections.<com.amazonaws.services.sqs.model.Message>emptyList()));

		PollableChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");

		//Act
		Message<?> receivedMessage = messageChannel.receive(0);

		//Assert
		Assert.assertNull(receivedMessage);
	}
}