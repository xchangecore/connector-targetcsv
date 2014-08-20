package com.saic.uicds.clients.em.targetcsv;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.TextType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.message.GenericMessage;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.em.async.UicdsIncident;
import com.saic.uicds.clients.em.async.UicdsWorkProduct;
import com.saic.uicds.clients.util.Common;

public class IncidentContentEnricher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

    HashMap<String, WorkProduct> currentWorkProducts;

    /**
     * @return the uicdsCore
     */
    public UicdsCore getUicdsCore() {

        return uicdsCore;
    }

    /**
     * @param uicdsCore the uicdsCore to set
     */
    public void setUicdsCore(UicdsCore uicdsCore) {

        this.uicdsCore = uicdsCore;
    }

    public IncidentContentEnricher() {

        currentWorkProducts = new HashMap<String, WorkProduct>();
    }

    @Transformer
    public Message<List<IncidentDocumentMessage>> enrichWithIncidentData(
        Message<Collection<IncidentDocumentMessage>> messages) {

        ArrayList<IncidentDocumentMessage> outboundMessages = new ArrayList<IncidentDocumentMessage>();

        // Get the current work products for all the incidents on the core
        updateCurrentWorkProductList();

        // Try to match each incoming message with an incident currently on the core
        for (IncidentDocumentMessage item : messages.getPayload()) {
            logger.debug("In the incoming message for loop, the store is: " + item.getData().getLocationNumber());
            WorkProductDocument incidentWorkProduct = lookForExistingIncident(item.getData());

            // If there is not a current work product for this Target item and this item is not marked
            // as closed then add
            if (incidentWorkProduct == null) {/** && !Utilities.tgIncidentIsClosed(item.getData())) {*/
                if (logger.isDebugEnabled()) {
                    logger.debug("No matching UICDS incident found for "
                        + item.getData().getLocationNumber());
                }
                outboundMessages.add(item);
            } else if (incidentWorkProduct != null && itemNeedsUpdate(item, incidentWorkProduct)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Matching UICDS incident found for " + item.getData().getLocationNumber());
                }
                item.setWorkProductDocument(incidentWorkProduct);
                outboundMessages.add(item);
            }

        }

        // Find any incidents that are missing from the incoming list and setup to
        // close and archive the incident
        ArrayList<IncidentDocumentMessage> incidentsToArchive = getIncidentsToRemove(messages);
        if (incidentsToArchive.size() > 0) {
            outboundMessages.addAll(incidentsToArchive);
        }

        return new GenericMessage<List<IncidentDocumentMessage>>(outboundMessages);
    }

    private boolean itemNeedsUpdate(IncidentDocumentMessage itemDoc,
        WorkProductDocument incidentWorkProduct) {

        IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(incidentWorkProduct.getWorkProduct());

        if (incident.getIncident().sizeOfActivityDescriptionTextArray() > 0){
            String description = incident.getIncident().getActivityDescriptionTextArray(0).toString();
            String delims = "<br/><b>";
            String[] tokens = description.split(delims);
            String itemDelims = "</b>";
            /*check status*/
            String[] statusToken = tokens[1].split(itemDelims);
            logger.debug("Checking Status Token, token 0 =" + statusToken[0]);        
            
            if (statusToken.length == 2) {
            	logger.debug("Status token 1 =" + statusToken[1]);
            	logger.debug("Actual Store Status =" + itemDoc.getData().getOperationStatus());
                if (!statusToken[1].equalsIgnoreCase(itemDoc.getData().getOperationStatus())){
                	logger.debug("Status token not the same");
         	   return true;
                }
            } else {
            	if ( itemDoc.getData().getOperationStatus() == null) {
            		logger.debug("Acutal Store status is Null");
            		return true;
            	}
            }
            	
            /*check Power Outage*/
            String[] powerOutageToken = tokens[2].split(itemDelims);
            logger.debug("Checking PowerOutage Token, token 0 =" + powerOutageToken[0]);
            if (powerOutageToken.length == 2){
            	logger.debug("Power Outage token 1 =" + powerOutageToken[1]);
            	logger.debug("Actual Power Outage =" + itemDoc.getData().getPowerOutageStatus());
               if (!powerOutageToken[1].equalsIgnoreCase(itemDoc.getData().getPowerOutageStatus())){
            	   logger.debug("PowerOutage token not the same");
         	   return true;
              }
            } else {
            	if (itemDoc.getData().getPowerOutageStatus() == null){
            		logger.debug("Acutal Power Outage Status is Null");
            		return true;
            	}
            }
            /*check Close Time*/
            String[] closeToken = tokens[3].split(itemDelims);
            logger.debug("Checking Close Token, token 0 =" + closeToken[0]);
            if (closeToken.length == 2){
            	logger.debug("Close token 1 =" + closeToken[1]);
            	logger.debug("Acutal Close =" + itemDoc.getData().getCloseTime());
                if (!closeToken[1].equalsIgnoreCase(itemDoc.getData().getCloseTime())){
                	logger.debug("Close Token not the same");
         	     return true;
                }
            } else {
            	if (itemDoc.getData().getCloseTime() == null){
            		logger.debug("Actual Close TIme is Null");
            		return true;
            	}
            }
            /*check Open Time*/
            String[] openToken = tokens[4].split(itemDelims);
            logger.debug("Checking Open Token, token 0 =" + openToken[0]);
            if (openToken.length == 2){
            	logger.debug("Open token 1 =" + openToken[1]);
            	logger.debug("Actual Generator =" + itemDoc.getData().getOpenTime());
               if (!openToken[1].equalsIgnoreCase(itemDoc.getData().getOpenTime())){
            	   logger.debug("Open Time not the same");
         	     return true;
               }
            } else {
            	if (itemDoc.getData().getOpenTime() == null){
            		logger.debug("Actual Open Time is Null");
            		return true;
            	}
            }
            /*check Facility Damage*/
            String[] damageToken = tokens[5].split(itemDelims);
            logger.debug("Checking Damage Token, token 0 =" + damageToken[0]);
            if (damageToken.length == 2){
            	logger.debug("Damage token 1 =" + damageToken[1]);
            	logger.debug("Actual Damage Level =" + itemDoc.getData().getFacilityDamage()+"<br/>");
                if (!damageToken[1].equalsIgnoreCase(itemDoc.getData().getFacilityDamage()+"<br/>")){
                	logger.debug("Damage Token Not the same");
         	      return true;
                }
            } else {
            	if (itemDoc.getData().getFacilityDamage() == null){
            		logger.debug("Actual Damage is Null");
            		return true;
            	}
            }
         }
        return false;
    }

    public ArrayList<IncidentDocumentMessage> getIncidentsToRemove(
        Message<Collection<IncidentDocumentMessage>> messages) {

        HashSet<String> set = new HashSet<String>();
        for (IncidentDocumentMessage message : messages.getPayload()) {
            if (message.getData().getLocationNumber() != null && !message.getData().getLocationNumber().isEmpty()) {
                set.add(message.getData().getLocationNumber());
            }
        }

        ArrayList<IncidentDocumentMessage> incidentsToArchive = new ArrayList<IncidentDocumentMessage>();
        for (String locationNumber : currentWorkProducts.keySet()) {
            if (!set.contains(locationNumber)) {
                IncidentDocumentMessage doc = new IncidentDocumentMessage();
                WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
                wpd.setWorkProduct(currentWorkProducts.get(locationNumber));
                doc.setWorkProductDocument(wpd);
                incidentsToArchive.add(doc);
                if (logger.isDebugEnabled()) {
                    logger.debug("Put this UICDS incident in archive list: " + locationNumber);
                }
            }
        }
        return incidentsToArchive;
    }

    private void updateCurrentWorkProductList() {
    	
    	WorkProductList incidentList = getIncidentList();

        if (incidentList != null) {
        logger.debug("The incident list is not null");
        // only clear the list if we got a valid incident list
        currentWorkProducts.clear();
        for (WorkProduct wp : incidentList.getWorkProductArray()) {
            IdentificationType id = Common.getIdentificationElement(wp);
            logger.debug("The IG ID for this WP is :" + id.getIdentifier().toString());
            IncidentDocument incidentDoc = getIncidentDocument(id);
            logger.debug(incidentDoc.getIncident().toString());
            if (incidentDoc != null && incidentDoc.getIncident() != null) {
                String storeID = findStoreID(incidentDoc.getIncident());
                if (storeID != null) {
                    if (wp.sizeOfStructuredPayloadArray() == 0) {
                        wp.addNewStructuredPayload();
                    }
                    wp.getStructuredPayloadArray(0).set(incidentDoc);
                    currentWorkProducts.put(storeID, wp);
                    logger.debug("Put Store ID in CurrentWorkProducts: "+ storeID);
                }
            }
        }
        
        }

    }

    private String findStoreID(UICDSIncidentType incident) {

        boolean foundTarget = false;
        String descriptionText;
        String delims = ":";
        
        if (incident != null && incident.sizeOfActivityCategoryTextArray() > 0) {
        	logger.debug("In findStoreID, first if check");
        	logger.debug(incident.toString());
            for (TextType category : incident.getActivityCategoryTextArray()) {
                  if (category.getStringValue().equalsIgnoreCase("Target Event")) {
                            foundTarget = true;
                            logger.debug("In findStoreID, found Target Event");
                        }
                    }
                }
                
                if (foundTarget) {
                    if (incident.sizeOfIncidentLocationArray() > 0) {
                        for (LocationType location : incident.getIncidentLocationArray()) {
                            if (location.sizeOfLocationAreaArray() > 0) {
                                if (location.getLocationAreaArray(0).sizeOfAreaCircularDescriptionTextArray() > 0) {
                                	  descriptionText = location.getLocationAreaArray(0).getAreaCircularDescriptionTextArray(0).getStringValue();
                                	  logger.debug("In findStoreID, descriptionText =" + descriptionText);
                                	  String[] tokens = descriptionText.split(delims);
                                	  logger.debug("In findStoreID, token 1 =" + tokens[1]);
                                	  return tokens[1];
                                }
                                    
                                    }
                                }
                            }
                        }
                    
            
        return null;
    }
    
    public WorkProductDocument lookForExistingIncident(TGItem item) {

        if (item.getLocationNumber() == null || item.getLocationNumber().isEmpty()) {
            logger.error("No Store No. found in the data item");
            return null;
        }

        String id = item.getLocationNumber();

        // Found an incident that was created from this Target item
        if (currentWorkProducts.containsKey(id)) {
            WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
            wpd.setWorkProduct(currentWorkProducts.get(id));
            logger.debug("Found an incident that was created from this Target item: " + id);
            return wpd;
        }

        return null;
    }

    private IncidentDocument getIncidentDocument(IdentificationType workProductIdentifier) {
        logger.debug("Asking the core for this WP Id =" + workProductIdentifier.toString());
        WorkProduct workProduct = uicdsCore.getWorkProductFromCore(workProductIdentifier);
        if (workProduct != null) {
        	logger.debug("Get Work Product Not Null");
            UicdsWorkProduct uicdsWorkProduct = new UicdsWorkProduct(workProduct);
            XmlObject content = uicdsWorkProduct.getContent(UicdsIncident.INCIDENT_SERVICE_NS,
                UicdsIncident.INCIDENT_ELEMENT_NAME);
            if (content != null) {
                try {
                    IncidentDocument incident = IncidentDocument.Factory.parse(content.getDomNode());
                    return incident;
                } catch (XmlException e) {
                    logger.error("Error parsing Incident document");
                    return null;
                }
            }
        }
        return null;
    }

    public WorkProductList getIncidentList() {

        GetIncidentListRequestDocument request = GetIncidentListRequestDocument.Factory.newInstance();
        request.addNewGetIncidentListRequest();
        GetIncidentListResponseDocument response = (GetIncidentListResponseDocument) uicdsCore.marshalSendAndReceive(request);

        if (response != null) {
        	logger.debug("IncidentContentEnricher.getIncidentList is not null");
            return response.getGetIncidentListResponse().getWorkProductList();
        } else {
            return null;
        }
    }

}
