package ru.nsu.ccfit.boltava.model.serializer;


import ru.nsu.ccfit.boltava.model.message.Message;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Serializes Message class instances into xml String and vice versa
 */
public class XmlMessageSerializer implements IMessageSerializer<String> {

    private static final String CONTEXT = "ru.nsu.ccfit.boltava.model.message";
    private final Marshaller mMarshaller;
    private final Unmarshaller mUnmarshaller;

    public XmlMessageSerializer() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(CONTEXT);
        mMarshaller = context.createMarshaller();
        mUnmarshaller = context.createUnmarshaller();
        mMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }

    public String serialize(Message message) throws JAXBException {
        StringWriter writer = new StringWriter();
        mMarshaller.marshal(message, writer);
        return writer.toString();
    }

    public Message deserialize(String xml) throws JAXBException {
        StringReader reader = new StringReader(xml);
        return (Message) mUnmarshaller.unmarshal(reader);
    }

}
