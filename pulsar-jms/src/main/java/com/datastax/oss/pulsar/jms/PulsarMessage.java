/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.pulsar.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import org.apache.pulsar.client.api.TypedMessageBuilder;

abstract class PulsarMessage implements Message {
  /**
   * Gets the message ID.
   *
   * <p>The {@code JMSMessageID} header field contains a value that uniquely identifies each message
   * sent by a provider.
   *
   * <p>When a message is sent, {@code JMSMessageID} can be ignored. When the {@code send} or {@code
   * publish} method returns, it contains a provider-assigned value.
   *
   * <p>A {@code JMSMessageID} is a {@code String} value that should function as a unique key for
   * identifying messages in a historical repository. The exact scope of uniqueness is
   * provider-defined. It should at least cover all messages for a specific installation of a
   * provider, where an installation is some connected set of message routers.
   *
   * <p>All {@code JMSMessageID} values must start with the prefix {@code 'ID:'}. Uniqueness of
   * message ID values across different providers is not required.
   *
   * <p>Since message IDs take some effort to create and increase a message's size, some JMS
   * providers may be able to optimize message overhead if they are given a hint that the message ID
   * is not used by an application. By calling the {@code MessageProducer.setDisableMessageID}
   * method, a JMS client enables this potential optimization for all messages sent by that message
   * producer. If the JMS provider accepts this hint, these messages must have the message ID set to
   * null; if the provider ignores the hint, the message ID must be set to its normal unique value.
   *
   * @return the message ID
   * @throws JMSException if the JMS provider fails to get the message ID due to some internal
   *     error.
   * @see Message#setJMSMessageID(String)
   * @see MessageProducer#setDisableMessageID(boolean)
   */
  @Override
  public String getJMSMessageID() throws JMSException {
    return null;
  }

  /**
   * Sets the message ID.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the message ID. This method is public to allow a
   * JMS provider to set this field when sending a message whose implementation is not its own.
   *
   * @param id the ID of the message
   * @throws JMSException if the JMS provider fails to set the message ID due to some internal
   *     error.
   * @see Message#getJMSMessageID()
   */
  @Override
  public void setJMSMessageID(String id) throws JMSException {}

  /**
   * Gets the message timestamp.
   *
   * <p>The {@code JMSTimestamp} header field contains the time a message was handed off to a
   * provider to be sent. It is not the time the message was actually transmitted, because the
   * actual send may occur later due to transactions or other client-side queueing of messages.
   *
   * <p>When a message is sent, {@code JMSTimestamp} is ignored. When the {@code send} or {@code
   * publish} method returns, it contains a time value somewhere in the interval between the call
   * and the return. The value is in the format of a normal millis time value in the Java
   * programming language.
   *
   * <p>Since timestamps take some effort to create and increase a message's size, some JMS
   * providers may be able to optimize message overhead if they are given a hint that the timestamp
   * is not used by an application. By calling the {@code
   * MessageProducer.setDisableMessageTimestamp} method, a JMS client enables this potential
   * optimization for all messages sent by that message producer. If the JMS provider accepts this
   * hint, these messages must have the timestamp set to zero; if the provider ignores the hint, the
   * timestamp must be set to its normal value.
   *
   * @return the message timestamp
   * @throws JMSException if the JMS provider fails to get the timestamp due to some internal error.
   * @see Message#setJMSTimestamp(long)
   * @see MessageProducer#setDisableMessageTimestamp(boolean)
   */
  @Override
  public long getJMSTimestamp() throws JMSException {
    return 0;
  }

  /**
   * Sets the message timestamp.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the message timestamp. This method is public to
   * allow a JMS provider to set this field when sending a message whose implementation is not its
   * own.
   *
   * @param timestamp the timestamp for this message
   * @throws JMSException if the JMS provider fails to set the timestamp due to some internal error.
   * @see Message#getJMSTimestamp()
   */
  @Override
  public void setJMSTimestamp(long timestamp) throws JMSException {}

  /**
   * Gets the correlation ID as an array of bytes for the message.
   *
   * <p>The use of a {@code byte[]} value for {@code JMSCorrelationID} is non-portable.
   *
   * @return the correlation ID of a message as an array of bytes
   * @throws JMSException if the JMS provider fails to get the correlation ID due to some internal
   *     error.
   * @see Message#setJMSCorrelationID(String)
   * @see Message#getJMSCorrelationID()
   * @see Message#setJMSCorrelationIDAsBytes(byte[])
   */
  @Override
  public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
    return new byte[0];
  }

  /**
   * Sets the correlation ID as an array of bytes for the message.
   *
   * <p>The array is copied before the method returns, so future modifications to the array will not
   * alter this message header.
   *
   * <p>If a provider supports the native concept of correlation ID, a JMS client may need to assign
   * specific {@code JMSCorrelationID} values to match those expected by native messaging clients.
   * JMS providers without native correlation ID values are not required to support this method and
   * its corresponding get method; their implementation may throw a {@code
   * java.lang.UnsupportedOperationException}.
   *
   * <p>The use of a {@code byte[]} value for {@code JMSCorrelationID} is non-portable.
   *
   * @param correlationID the correlation ID value as an array of bytes
   * @throws JMSException if the JMS provider fails to set the correlation ID due to some internal
   *     error.
   * @see Message#setJMSCorrelationID(String)
   * @see Message#getJMSCorrelationID()
   * @see Message#getJMSCorrelationIDAsBytes()
   */
  @Override
  public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {}

  /**
   * Sets the correlation ID for the message.
   *
   * <p>A client can use the {@code JMSCorrelationID} header field to link one message with another.
   * A typical use is to link a response message with its request message.
   *
   * <p>{@code JMSCorrelationID} can hold one of the following:
   *
   * <ul>
   *   <li>A provider-specific message ID
   *   <li>An application-specific {@code String}
   *   <li>A provider-native {@code byte[]} value
   * </ul>
   *
   * <p>Since each message sent by a JMS provider is assigned a message ID value, it is convenient
   * to link messages via message ID. All message ID values must start with the {@code 'ID:'}
   * prefix.
   *
   * <p>In some cases, an application (made up of several clients) needs to use an
   * application-specific value for linking messages. For instance, an application may use {@code
   * JMSCorrelationID} to hold a value referencing some external information. Application-specified
   * values must not start with the {@code 'ID:'} prefix; this is reserved for provider-generated
   * message ID values.
   *
   * <p>If a provider supports the native concept of correlation ID, a JMS client may need to assign
   * specific {@code JMSCorrelationID} values to match those expected by clients that do not use the
   * JMS API. A {@code byte[]} value is used for this purpose. JMS providers without native
   * correlation ID values are not required to support {@code byte[]} values. The use of a {@code
   * byte[]} value for {@code JMSCorrelationID} is non-portable.
   *
   * @param correlationID the message ID of a message being referred to
   * @throws JMSException if the JMS provider fails to set the correlation ID due to some internal
   *     error.
   * @see Message#getJMSCorrelationID()
   * @see Message#getJMSCorrelationIDAsBytes()
   * @see Message#setJMSCorrelationIDAsBytes(byte[])
   */
  @Override
  public void setJMSCorrelationID(String correlationID) throws JMSException {}

  /**
   * Gets the correlation ID for the message.
   *
   * <p>This method is used to return correlation ID values that are either provider-specific
   * message IDs or application-specific {@code String} values.
   *
   * @return the correlation ID of a message as a {@code String}
   * @throws JMSException if the JMS provider fails to get the correlation ID due to some internal
   *     error.
   * @see Message#setJMSCorrelationID(String)
   * @see Message#getJMSCorrelationIDAsBytes()
   * @see Message#setJMSCorrelationIDAsBytes(byte[])
   */
  @Override
  public String getJMSCorrelationID() throws JMSException {
    return null;
  }

  /**
   * Gets the {@code Destination} object to which a reply to this message should be sent.
   *
   * @return {@code Destination} to which to send a response to this message
   * @throws JMSException if the JMS provider fails to get the {@code JMSReplyTo} destination due to
   *     some internal error.
   * @see Message#setJMSReplyTo(Destination)
   */
  @Override
  public Destination getJMSReplyTo() throws JMSException {
    return null;
  }

  /**
   * Sets the {@code Destination} object to which a reply to this message should be sent.
   *
   * <p>The {@code JMSReplyTo} header field contains the destination where a reply to the current
   * message should be sent. If it is null, no reply is expected. The destination may be either a
   * {@code Queue} object or a {@code Topic} object.
   *
   * <p>Messages sent with a null {@code JMSReplyTo} value may be a notification of some event, or
   * they may just be some data the sender thinks is of interest.
   *
   * <p>Messages with a {@code JMSReplyTo} value typically expect a response. A response is
   * optional; it is up to the client to decide. These messages are called requests. A message sent
   * in response to a request is called a reply.
   *
   * <p>In some cases a client may wish to match a request it sent earlier with a reply it has just
   * received. The client can use the {@code JMSCorrelationID} header field for this purpose.
   *
   * @param replyTo {@code Destination} to which to send a response to this message
   * @throws JMSException if the JMS provider fails to set the {@code JMSReplyTo} destination due to
   *     some internal error.
   * @see Message#getJMSReplyTo()
   */
  @Override
  public void setJMSReplyTo(Destination replyTo) throws JMSException {}

  /**
   * Gets the {@code Destination} object for this message.
   *
   * <p>The {@code JMSDestination} header field contains the destination to which the message is
   * being sent.
   *
   * <p>When a message is sent, this field is ignored. After completion of the {@code send} or
   * {@code publish} method, the field holds the destination specified by the method.
   *
   * <p>When a message is received, its {@code JMSDestination} value must be equivalent to the value
   * assigned when it was sent.
   *
   * @return the destination of this message
   * @throws JMSException if the JMS provider fails to get the destination due to some internal
   *     error.
   * @see Message#setJMSDestination(Destination)
   */
  @Override
  public Destination getJMSDestination() throws JMSException {
    return null;
  }

  /**
   * Sets the {@code Destination} object for this message.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the destination of the message. This method is
   * public to allow a JMS provider to set this field when sending a message whose implementation is
   * not its own.
   *
   * @param destination the destination for this message
   * @throws JMSException if the JMS provider fails to set the destination due to some internal
   *     error.
   * @see Message#getJMSDestination()
   */
  @Override
  public void setJMSDestination(Destination destination) throws JMSException {}

  /**
   * Gets the {@code DeliveryMode} value specified for this message.
   *
   * @return the delivery mode for this message
   * @throws JMSException if the JMS provider fails to get the delivery mode due to some internal
   *     error.
   * @see Message#setJMSDeliveryMode(int)
   * @see DeliveryMode
   */
  @Override
  public int getJMSDeliveryMode() throws JMSException {
    return 0;
  }

  /**
   * Sets the {@code DeliveryMode} value for this message.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the delivery mode of the message. This method is
   * public to allow a JMS provider to set this field when sending a message whose implementation is
   * not its own.
   *
   * @param deliveryMode the delivery mode for this message
   * @throws JMSException if the JMS provider fails to set the delivery mode due to some internal
   *     error.
   * @see Message#getJMSDeliveryMode()
   * @see DeliveryMode
   */
  @Override
  public void setJMSDeliveryMode(int deliveryMode) throws JMSException {}

  /**
   * Gets an indication of whether this message is being redelivered.
   *
   * <p>If a client receives a message with the {@code JMSRedelivered} field set, it is likely, but
   * not guaranteed, that this message was delivered earlier but that its receipt was not
   * acknowledged at that time.
   *
   * @return true if this message is being redelivered
   * @throws JMSException if the JMS provider fails to get the redelivered state due to some
   *     internal error.
   * @see Message#setJMSRedelivered(boolean)
   */
  @Override
  public boolean getJMSRedelivered() throws JMSException {
    return false;
  }

  /**
   * Specifies whether this message is being redelivered.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is delivered.
   * This message cannot be used by clients to configure the redelivered status of the message. This
   * method is public to allow a JMS provider to set this field when sending a message whose
   * implementation is not its own.
   *
   * @param redelivered an indication of whether this message is being redelivered
   * @throws JMSException if the JMS provider fails to set the redelivered state due to some
   *     internal error.
   * @see Message#getJMSRedelivered()
   */
  @Override
  public void setJMSRedelivered(boolean redelivered) throws JMSException {}

  /**
   * Gets the message type identifier supplied by the client when the message was sent.
   *
   * @return the message type
   * @throws JMSException if the JMS provider fails to get the message type due to some internal
   *     error.
   * @see Message#setJMSType(String)
   */
  @Override
  public String getJMSType() throws JMSException {
    return null;
  }

  /**
   * Sets the message type.
   *
   * <p>Some JMS providers use a message repository that contains the definitions of messages sent
   * by applications. The {@code JMSType} header field may reference a message's definition in the
   * provider's repository.
   *
   * <p>The JMS API does not define a standard message definition repository, nor does it define a
   * naming policy for the definitions it contains.
   *
   * <p>Some messaging systems require that a message type definition for each application message
   * be created and that each message specify its type. In order to work with such JMS providers,
   * JMS clients should assign a value to {@code JMSType}, whether the application makes use of it
   * or not. This ensures that the field is properly set for those providers that require it.
   *
   * <p>To ensure portability, JMS clients should use symbolic values for {@code JMSType} that can
   * be configured at installation time to the values defined in the current provider's message
   * repository. If string literals are used, they may not be valid type names for some JMS
   * providers.
   *
   * @param type the message type
   * @throws JMSException if the JMS provider fails to set the message type due to some internal
   *     error.
   * @see Message#getJMSType()
   */
  @Override
  public void setJMSType(String type) throws JMSException {}

  /**
   * Gets the message's expiration time.
   *
   * <p>When a message is sent, the {@code JMSExpiration} header field is left unassigned. After
   * completion of the {@code send} or {@code publish} method, it holds the expiration time of the
   * message. This is the the difference, measured in milliseconds, between the expiration time and
   * midnight, January 1, 1970 UTC.
   *
   * <p>If the time-to-live is specified as zero, {@code JMSExpiration} is set to zero to indicate
   * that the message does not expire.
   *
   * <p>When a message's expiration time is reached, a provider should discard it. The JMS API does
   * not define any form of notification of message expiration.
   *
   * <p>Clients should not receive messages that have expired; however, the JMS API does not
   * guarantee that this will not happen.
   *
   * @return the message's expiration time value
   * @throws JMSException if the JMS provider fails to get the message expiration due to some
   *     internal error.
   * @see Message#setJMSExpiration(long)
   */
  @Override
  public long getJMSExpiration() throws JMSException {
    return 0;
  }

  /**
   * Sets the message's expiration value.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the expiration time of the message. This method
   * is public to allow a JMS provider to set this field when sending a message whose implementation
   * is not its own.
   *
   * @param expiration the message's expiration time
   * @throws JMSException if the JMS provider fails to set the message expiration due to some
   *     internal error.
   * @see Message#getJMSExpiration()
   */
  @Override
  public void setJMSExpiration(long expiration) throws JMSException {}

  /**
   * Gets the message's delivery time value.
   *
   * <p>When a message is sent, the {@code JMSDeliveryTime} header field is left unassigned. After
   * completion of the {@code send} or {@code publish} method, it holds the delivery time of the
   * message. This is the the difference, measured in milliseconds, between the delivery time and
   * midnight, January 1, 1970 UTC.
   *
   * <p>A message's delivery time is the earliest time when a JMS provider may deliver the message
   * to a consumer. The provider must not deliver messages before the delivery time has been
   * reached.
   *
   * @return the message's delivery time value
   * @throws JMSException if the JMS provider fails to get the delivery time due to some internal
   *     error.
   * @see Message#setJMSDeliveryTime(long)
   * @since JMS 2.0
   */
  @Override
  public long getJMSDeliveryTime() throws JMSException {
    return 0;
  }

  /**
   * Sets the message's delivery time value.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the delivery time of the message. This method is
   * public to allow a JMS provider to set this field when sending a message whose implementation is
   * not its own.
   *
   * @param deliveryTime the message's delivery time value
   * @throws JMSException if the JMS provider fails to set the delivery time due to some internal
   *     error.
   * @see Message#getJMSDeliveryTime()
   * @since JMS 2.0
   */
  @Override
  public void setJMSDeliveryTime(long deliveryTime) throws JMSException {}

  /**
   * Gets the message priority level.
   *
   * <p>The JMS API defines ten levels of priority value, with 0 as the lowest priority and 9 as the
   * highest. In addition, clients should consider priorities 0-4 as gradations of normal priority
   * and priorities 5-9 as gradations of expedited priority.
   *
   * <p>The JMS API does not require that a provider strictly implement priority ordering of
   * messages; however, it should do its best to deliver expedited messages ahead of normal
   * messages.
   *
   * @return the default message priority
   * @throws JMSException if the JMS provider fails to get the message priority due to some internal
   *     error.
   * @see Message#setJMSPriority(int)
   */
  @Override
  public int getJMSPriority() throws JMSException {
    return 0;
  }

  /**
   * Sets the priority level for this message.
   *
   * <p>This method is for use by JMS providers only to set this field when a message is sent. This
   * message cannot be used by clients to configure the priority level of the message. This method
   * is public to allow a JMS provider to set this field when sending a message whose implementation
   * is not its own.
   *
   * @param priority the priority of this message
   * @throws JMSException if the JMS provider fails to set the message priority due to some internal
   *     error.
   * @see Message#getJMSPriority()
   */
  @Override
  public void setJMSPriority(int priority) throws JMSException {}

  /**
   * Clears a message's properties.
   *
   * <p>The message's header fields and body are not cleared.
   *
   * @throws JMSException if the JMS provider fails to clear the message properties due to some
   *     internal error.
   */
  @Override
  public void clearProperties() throws JMSException {}

  /**
   * Indicates whether a property value exists.
   *
   * @param name the name of the property to test
   * @return true if the property exists
   * @throws JMSException if the JMS provider fails to determine if the property exists due to some
   *     internal error.
   */
  @Override
  public boolean propertyExists(String name) throws JMSException {
    return false;
  }

  /**
   * Returns the value of the {@code boolean} property with the specified name.
   *
   * @param name the name of the {@code boolean} property
   * @return the {@code boolean} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public boolean getBooleanProperty(String name) throws JMSException {
    return false;
  }

  /**
   * Returns the value of the {@code byte} property with the specified name.
   *
   * @param name the name of the {@code byte} property
   * @return the {@code byte} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public byte getByteProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code short} property with the specified name.
   *
   * @param name the name of the {@code short} property
   * @return the {@code short} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public short getShortProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code int} property with the specified name.
   *
   * @param name the name of the {@code int} property
   * @return the {@code int} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public int getIntProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code long} property with the specified name.
   *
   * @param name the name of the {@code long} property
   * @return the {@code long} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public long getLongProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code float} property with the specified name.
   *
   * @param name the name of the {@code float} property
   * @return the {@code float} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public float getFloatProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code double} property with the specified name.
   *
   * @param name the name of the {@code double} property
   * @return the {@code double} property value for the specified name
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public double getDoubleProperty(String name) throws JMSException {
    return 0;
  }

  /**
   * Returns the value of the {@code String} property with the specified name.
   *
   * @param name the name of the {@code String} property
   * @return the {@code String} property value for the specified name; if there is no property by
   *     this name, a null value is returned
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   * @throws MessageFormatException if this type conversion is invalid.
   */
  @Override
  public String getStringProperty(String name) throws JMSException {
    return null;
  }

  /**
   * Returns the value of the Java object property with the specified name.
   *
   * <p>This method can be used to return, in objectified format, an object that has been stored as
   * a property in the message with the equivalent <code>setObjectProperty</code> method call, or
   * its equivalent primitive <code>set<I>type</I>Property</code> method.
   *
   * @param name the name of the Java object property
   * @return the Java object property value with the specified name, in objectified format (for
   *     example, if the property was set as an {@code int}, an {@code Integer} is returned); if
   *     there is no property by this name, a null value is returned
   * @throws JMSException if the JMS provider fails to get the property value due to some internal
   *     error.
   */
  @Override
  public Object getObjectProperty(String name) throws JMSException {
    return null;
  }

  /**
   * Returns an {@code Enumeration} of all the property names.
   *
   * <p>Note that JMS standard header fields are not considered properties and are not returned in
   * this enumeration.
   *
   * @return an enumeration of all the names of property values
   * @throws JMSException if the JMS provider fails to get the property names due to some internal
   *     error.
   */
  @Override
  public Enumeration getPropertyNames() throws JMSException {
    return null;
  }

  /**
   * Sets a {@code boolean} property value with the specified name into the message.
   *
   * @param name the name of the {@code boolean} property
   * @param value the {@code boolean} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setBooleanProperty(String name, boolean value) throws JMSException {}

  /**
   * Sets a {@code byte} property value with the specified name into the message.
   *
   * @param name the name of the {@code byte} property
   * @param value the {@code byte} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setByteProperty(String name, byte value) throws JMSException {}

  /**
   * Sets a {@code short} property value with the specified name into the message.
   *
   * @param name the name of the {@code short} property
   * @param value the {@code short} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setShortProperty(String name, short value) throws JMSException {}

  /**
   * Sets an {@code int} property value with the specified name into the message.
   *
   * @param name the name of the {@code int} property
   * @param value the {@code int} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setIntProperty(String name, int value) throws JMSException {}

  /**
   * Sets a {@code long} property value with the specified name into the message.
   *
   * @param name the name of the {@code long} property
   * @param value the {@code long} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setLongProperty(String name, long value) throws JMSException {}

  /**
   * Sets a {@code float} property value with the specified name into the message.
   *
   * @param name the name of the {@code float} property
   * @param value the {@code float} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setFloatProperty(String name, float value) throws JMSException {}

  /**
   * Sets a {@code double} property value with the specified name into the message.
   *
   * @param name the name of the {@code double} property
   * @param value the {@code double} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setDoubleProperty(String name, double value) throws JMSException {}

  /**
   * Sets a {@code String} property value with the specified name into the message.
   *
   * @param name the name of the {@code String} property
   * @param value the {@code String} property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setStringProperty(String name, String value) throws JMSException {}

  /**
   * Sets a Java object property value with the specified name into the message.
   *
   * <p>Note that this method works only for the objectified primitive object types ({@code
   * Integer}, {@code Double}, {@code Long} ...) and {@code String} objects.
   *
   * @param name the name of the Java object property
   * @param value the Java object property value to set
   * @throws JMSException if the JMS provider fails to set the property due to some internal error.
   * @throws IllegalArgumentException if the name is null or if the name is an empty string.
   * @throws MessageFormatException if the object is invalid
   * @throws MessageNotWriteableException if properties are read-only
   */
  @Override
  public void setObjectProperty(String name, Object value) throws JMSException {}

  /**
   * Acknowledges all consumed messages of the session of this consumed message.
   *
   * <p>All consumed JMS messages support the {@code acknowledge} method for use when a client has
   * specified that its JMS session's consumed messages are to be explicitly acknowledged. By
   * invoking {@code acknowledge} on a consumed message, a client acknowledges all messages consumed
   * by the session that the message was delivered to.
   *
   * <p>Calls to {@code acknowledge} are ignored for both transacted sessions and sessions specified
   * to use implicit acknowledgement modes.
   *
   * <p>A client may individually acknowledge each message as it is consumed, or it may choose to
   * acknowledge messages as an application-defined group (which is done by calling acknowledge on
   * the last received message of the group, thereby acknowledging all messages consumed by the
   * session.)
   *
   * <p>Messages that have been received but not acknowledged may be redelivered.
   *
   * @throws JMSException if the JMS provider fails to acknowledge the messages due to some internal
   *     error.
   * @throws IllegalStateException if this method is called on a closed session.
   * @see Session#CLIENT_ACKNOWLEDGE
   */
  @Override
  public void acknowledge() throws JMSException {}

  /**
   * Clears out the message body. Clearing a message's body does not clear its header values or
   * property entries.
   *
   * <p>If this message body was read-only, calling this method leaves the message body in the same
   * state as an empty body in a newly created message.
   *
   * @throws JMSException if the JMS provider fails to clear the message body due to some internal
   *     error.
   */
  @Override
  public void clearBody() throws JMSException {}

  /**
   * Returns the message body as an object of the specified type. This method may be called on any
   * type of message except for <tt>StreamMessage</tt>. The message body must be capable of being
   * assigned to the specified type. This means that the specified class or interface must be either
   * the same as, or a superclass or superinterface of, the class of the message body. If the
   * message has no body then any type may be specified and null is returned.
   *
   * <p>
   *
   * @param c The type to which the message body will be assigned. <br>
   *     If the message is a {@code TextMessage} then this parameter must be set to {@code
   *     String.class} or another type to which a {@code String} is assignable. <br>
   *     If the message is a {@code ObjectMessage} then parameter must must be set to {@code
   *     java.io.Serializable.class} or another type to which the body is assignable. <br>
   *     If the message is a {@code MapMessage} then this parameter must be set to {@code
   *     java.util.Map.class} (or {@code java.lang.Object.class}). <br>
   *     If the message is a {@code BytesMessage} then this parameter must be set to {@code
   *     byte[].class} (or {@code java.lang.Object.class}). This method will reset the {@code
   *     BytesMessage} before and after use.<br>
   *     If the message is a {@code TextMessage}, {@code ObjectMessage}, {@code MapMessage} or
   *     {@code BytesMessage} and the message has no body, then the above does not apply and this
   *     parameter may be set to any type; the returned value will always be null.<br>
   *     If the message is a {@code Message} (but not one of its subtypes) then this parameter may
   *     be set to any type; the returned value will always be null.
   * @return the message body
   * @throws MessageFormatException
   *     <ul>
   *       <li>if the message is a {@code StreamMessage}
   *       <li>if the message body cannot be assigned to the specified type
   *       <li>if the message is an {@code ObjectMessage} and object deserialization fails.
   *     </ul>
   *
   * @throws JMSException if the JMS provider fails to get the message body due to some internal
   *     error.
   * @since JMS 2.0
   */
  @Override
  public <T> T getBody(Class<T> c) throws JMSException {
    return null;
  }

  /**
   * Returns whether the message body is capable of being assigned to the specified type. If this
   * method returns true then a subsequent call to the method {@code getBody} on the same message
   * with the same type argument would not throw a MessageFormatException.
   *
   * <p>If the message is a {@code StreamMessage} then false is always returned. If the message is a
   * {@code ObjectMessage} and object deserialization fails then false is returned. If the message
   * has no body then any type may be specified and true is returned.
   *
   * @param c The specified type <br>
   *     If the message is a {@code TextMessage} then this method will only return true if this
   *     parameter is set to {@code String.class} or another type to which a {@code String} is
   *     assignable. <br>
   *     If the message is a {@code ObjectMessage} then this method will only return true if this
   *     parameter is set to {@code java.io.Serializable.class} or another class to which the body
   *     is assignable. <br>
   *     If the message is a {@code MapMessage} then this method will only return true if this
   *     parameter is set to {@code java.util.Map.class} (or {@code java.lang.Object.class}). <br>
   *     If the message is a {@code BytesMessage} then this this method will only return true if
   *     this parameter is set to {@code byte[].class} (or {@code java.lang.Object.class}). <br>
   *     If the message is a {@code TextMessage}, {@code ObjectMessage}, {@code MapMessage} or
   *     {@code BytesMessage} and the message has no body, then the above does not apply and this
   *     method will return true irrespective of the value of this parameter.<br>
   *     If the message is a {@code Message} (but not one of its subtypes) then this method will
   *     return true irrespective of the value of this parameter.
   * @return whether the message body is capable of being assigned to the specified type
   * @throws JMSException if the JMS provider fails to return a value due to some internal error.
   */
  @Override
  public abstract boolean isBodyAssignableTo(Class c) throws JMSException;

  final void send(TypedMessageBuilder<byte[]> producer) throws JMSException {
    prepareForSend(producer);
    Utils.invoke(() -> producer.send());
  }

  abstract void prepareForSend(TypedMessageBuilder<byte[]> producer) throws JMSException;

  static final class PulsarStreamMessage extends PulsarMessage implements StreamMessage {

    private ByteArrayOutputStream stream;
    private byte[] originalMessage;
    private ObjectInputStream dataInputStream;
    private ObjectOutputStream dataOutputStream;

    PulsarStreamMessage(byte[] payload) throws JMSException {
      try {
        this.dataInputStream = new ObjectInputStream(new ByteArrayInputStream(payload));
        this.originalMessage = payload;
        this.stream = null;
        this.dataOutputStream = null;
      } catch (Exception err) {
        throw Utils.handleException(err);
      }
    }

    PulsarStreamMessage() throws JMSException {
      try {
        this.dataInputStream = null;
        this.stream = new ByteArrayOutputStream();
        this.dataOutputStream = new ObjectOutputStream(stream);
        this.originalMessage = null;
      } catch (Exception err) {
        throw Utils.handleException(err);
      }
    }

    /**
     * Returns whether the message body is capable of being assigned to the specified type. If this
     * method returns true then a subsequent call to the method {@code getBody} on the same message
     * with the same type argument would not throw a MessageFormatException.
     *
     * <p>If the message is a {@code StreamMessage} then false is always returned. If the message is
     * a {@code ObjectMessage} and object deserialization fails then false is returned. If the
     * message has no body then any type may be specified and true is returned.
     *
     * @param c The specified type <br>
     *     If the message is a {@code TextMessage} then this method will only return true if this
     *     parameter is set to {@code String.class} or another type to which a {@code String} is
     *     assignable. <br>
     *     If the message is a {@code ObjectMessage} then this method will only return true if this
     *     parameter is set to {@code java.io.Serializable.class} or another class to which the body
     *     is assignable. <br>
     *     If the message is a {@code MapMessage} then this method will only return true if this
     *     parameter is set to {@code java.util.Map.class} (or {@code java.lang.Object.class}). <br>
     *     If the message is a {@code BytesMessage} then this this method will only return true if
     *     this parameter is set to {@code byte[].class} (or {@code java.lang.Object.class}). <br>
     *     If the message is a {@code TextMessage}, {@code ObjectMessage}, {@code MapMessage} or
     *     {@code BytesMessage} and the message has no body, then the above does not apply and this
     *     method will return true irrespective of the value of this parameter.<br>
     *     If the message is a {@code Message} (but not one of its subtypes) then this method will
     *     return true irrespective of the value of this parameter.
     * @return whether the message body is capable of being assigned to the specified type
     * @throws JMSException if the JMS provider fails to return a value due to some internal error.
     */
    @Override
    public boolean isBodyAssignableTo(Class c) throws JMSException {
      return byte[].class == c;
    }

    @Override
    void prepareForSend(TypedMessageBuilder<byte[]> producer) throws JMSException {
      if (stream != null) {
        // write mode
        producer.value(stream.toByteArray());
      } else {
        // read mode
        producer.value(originalMessage);
      }
    }

    /**
     * Reads a {@code boolean} from the stream message.
     *
     * @return the {@code boolean} value read
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public boolean readBoolean() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readBoolean();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    private static <T> T handleException(Throwable t) throws JMSException {
      if (t instanceof EOFException) {
        throw new MessageEOFException(t + "");
      }
      throw Utils.handleException(t);
    }

    /**
     * Reads a {@code byte} value from the stream message.
     *
     * @return the next byte from the stream message as a 8-bit {@code byte}
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public byte readByte() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readByte();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a 16-bit integer from the stream message.
     *
     * @return a 16-bit integer from the stream message
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public short readShort() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readShort();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a Unicode character value from the stream message.
     *
     * @return a Unicode character from the stream message
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public char readChar() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readChar();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a 32-bit integer from the stream message.
     *
     * @return a 32-bit integer value from the stream message, interpreted as an {@code int}
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public int readInt() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readInt();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a 64-bit integer from the stream message.
     *
     * @return a 64-bit integer value from the stream message, interpreted as a {@code long}
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public long readLong() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readLong();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a {@code float} from the stream message.
     *
     * @return a {@code float} value from the stream message
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public float readFloat() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readFloat();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a {@code double} from the stream message.
     *
     * @return a {@code double} value from the stream message
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public double readDouble() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readDouble();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a {@code String} from the stream message.
     *
     * @return a Unicode string from the stream message
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     */
    @Override
    public String readString() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readUTF();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads a byte array field from the stream message into the specified {@code byte[]} object
     * (the read buffer).
     *
     * <p>To read the field value, {@code readBytes} should be successively called until it returns
     * a value less than the length of the read buffer. The value of the bytes in the buffer
     * following the last byte read is undefined.
     *
     * <p>If {@code readBytes} returns a value equal to the length of the buffer, a subsequent
     * {@code readBytes} call must be made. If there are no more bytes to be read, this call returns
     * -1.
     *
     * <p>If the byte array field value is null, {@code readBytes} returns -1.
     *
     * <p>If the byte array field value is empty, {@code readBytes} returns 0.
     *
     * <p>Once the first {@code readBytes} call on a {@code byte[]} field value has been made, the
     * full value of the field must be read before it is valid to read the next field. An attempt to
     * read the next field before that has been done will throw a {@code MessageFormatException}.
     *
     * <p>To read the byte field value into a new {@code byte[]} object, use the {@code readObject}
     * method.
     *
     * @param value the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *     because the end of the byte field has been reached
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     * @see #readObject()
     */
    @Override
    public int readBytes(byte[] value) throws JMSException {
      checkReadable();
      if (value == null) {
        return -1;
      }
      try {
        return dataInputStream.read(value);
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Reads an object from the stream message.
     *
     * <p>This method can be used to return, in objectified format, an object in the Java
     * programming language ("Java object") that has been written to the stream with the equivalent
     * {@code writeObject} method call, or its equivalent primitive <code>write<I>type</I></code>
     * method.
     *
     * <p>Note that byte values are returned as {@code byte[]}, not {@code Byte[]}.
     *
     * <p>An attempt to call {@code readObject} to read a byte field value into a new {@code byte[]}
     * object before the full value of the byte field has been read will throw a {@code
     * MessageFormatException}.
     *
     * @return a Java object from the stream message, in objectified format (for example, if the
     *     object was written as an {@code int}, an {@code Integer} is returned)
     * @throws JMSException if the JMS provider fails to read the message due to some internal
     *     error.
     * @throws MessageEOFException if unexpected end of message stream has been reached.
     * @throws MessageFormatException if this type conversion is invalid.
     * @throws MessageNotReadableException if the message is in write-only mode.
     * @see #readBytes(byte[] value)
     */
    @Override
    public Object readObject() throws JMSException {
      checkReadable();
      try {
        return dataInputStream.readUnshared();
      } catch (Exception err) {
        return handleException(err);
      }
    }

    /**
     * Writes a {@code boolean} to the stream message. The value {@code true} is written as the
     * value {@code (byte)1}; the value {@code false} is written as the value {@code (byte)0}.
     *
     * @param value the {@code boolean} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeBoolean(boolean value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeBoolean(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    private void checkWritable() throws MessageNotWriteableException {
      if (dataOutputStream == null) throw new MessageNotWriteableException("not writable");
    }

    private void checkReadable() throws MessageNotReadableException {
      if (dataInputStream == null) throw new MessageNotReadableException("not readable");
    }

    /**
     * Writes a {@code byte} to the stream message.
     *
     * @param value the {@code byte} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeByte(byte value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeByte(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code short} to the stream message.
     *
     * @param value the {@code short} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeShort(short value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeShort(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code char} to the stream message.
     *
     * @param value the {@code char} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeChar(char value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeChar(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes an {@code int} to the stream message.
     *
     * @param value the {@code int} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeInt(int value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeInt(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code long} to the stream message.
     *
     * @param value the {@code long} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeLong(long value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeLong(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code float} to the stream message.
     *
     * @param value the {@code float} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeFloat(float value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeFloat(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code double} to the stream message.
     *
     * @param value the {@code double} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeDouble(double value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeDouble(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a {@code String} to the stream message.
     *
     * @param value the {@code String} value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeString(String value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeUTF(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a byte array field to the stream message.
     *
     * <p>The byte array {@code value} is written to the message as a byte array field.
     * Consecutively written byte array fields are treated as two distinct fields when the fields
     * are read.
     *
     * @param value the byte array value to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeBytes(byte[] value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.write(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes a portion of a byte array as a byte array field to the stream message.
     *
     * <p>The a portion of the byte array {@code value} is written to the message as a byte array
     * field. Consecutively written byte array fields are treated as two distinct fields when the
     * fields are read.
     *
     * @param value the byte array value to be written
     * @param offset the initial offset within the byte array
     * @param length the number of bytes to use
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.write(value, offset, length);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Writes an object to the stream message.
     *
     * <p>This method works only for the objectified primitive object types ({@code Integer}, {@code
     * Double}, {@code Long}&nbsp;...), {@code String} objects, and byte arrays.
     *
     * @param value the Java object to be written
     * @throws JMSException if the JMS provider fails to write the message due to some internal
     *     error.
     * @throws MessageFormatException if the object is invalid.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void writeObject(Object value) throws JMSException {
      checkWritable();
      try {
        dataOutputStream.writeUnshared(value);
      } catch (Exception err) {
        handleException(err);
      }
    }

    /**
     * Puts the message body in read-only mode and repositions the stream to the beginning.
     *
     * @throws JMSException if the JMS provider fails to reset the message due to some internal
     *     error.
     * @throws MessageFormatException if the message has an invalid format.
     */
    @Override
    public void reset() throws JMSException {
      try {
        if (stream != null) {
          this.dataInputStream =
              new ObjectInputStream(new ByteArrayInputStream(stream.toByteArray()));
          this.stream = null;
          this.dataOutputStream = null;
        } else {
          this.dataInputStream = new ObjectInputStream(new ByteArrayInputStream(originalMessage));
        }
      } catch (Exception err) {
        handleException(err);
      }
    }
  }

  static final class PulsarTextMessage extends PulsarMessage implements TextMessage {
    private String text;

    public PulsarTextMessage(String text) {
      this.text = text;
    }

    @Override
    public boolean isBodyAssignableTo(Class c) {
      return c == String.class;
    }

    @Override
    void prepareForSend(TypedMessageBuilder<byte[]> producer) throws JMSException {
      producer.value(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sets the string containing this message's data.
     *
     * @param string the {@code String} containing the message's data
     * @throws JMSException if the JMS provider fails to set the text due to some internal error.
     * @throws MessageNotWriteableException if the message is in read-only mode.
     */
    @Override
    public void setText(String string) throws JMSException {
      this.text = string;
    }

    /**
     * Gets the string containing this message's data. The default value is null.
     *
     * @return the {@code String} containing the message's data
     * @throws JMSException if the JMS provider fails to get the text due to some internal error.
     */
    @Override
    public String getText() throws JMSException {
      return text;
    }
  }
}