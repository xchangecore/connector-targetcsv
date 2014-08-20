package com.saic.uicds.clients.em.targetcsv;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;

public class EventAggregator {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // @Transformer
    public Message<Collection<IncidentDocumentMessage>> passthrough(
        Message<List<TGItem>> message) {

        List<TGItem> items = message.getPayload();

        Map<String, IncidentDocumentMessage> documents = new HashMap<String, IncidentDocumentMessage>();

        if (items.size() == 0) {
            return new GenericMessage<Collection<IncidentDocumentMessage>>(documents.values());
        }

        for (TGItem item : items) {

                IncidentDocumentMessage docMessage = new IncidentDocumentMessage();
                docMessage.setTgdata(item);
                docMessage = addEvent(docMessage, item);
                documents.put(item.getLocationNumber(), docMessage);
                logger.debug("EventAggregator Putting in the document: "+ item.getLocationNumber());
            }
       

        /*if (logger.isDebugEnabled() && items.size() > 0) {
            for (String key : documents.keySet()) {
                logger.debug("Got " + documents.get(key).getEvents().size()
                    + " events for incident " + documents.get(key).getData().getNum_1());
            }
        }*/

        return new GenericMessage<Collection<IncidentDocumentMessage>>(documents.values());

    }

    
    private IncidentDocumentMessage addEvent(IncidentDocumentMessage docMessage, TGItem event) {

        docMessage.addEvent(event);
        return docMessage;
    }
}
