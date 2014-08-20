package com.saic.uicds.clients.em.targetcsv;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.TextType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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



public class IncidentManager {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

    HashMap<String, WorkProduct> currentTargetIncidentWorkProducts;
    

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

    public IncidentManager() {

        currentTargetIncidentWorkProducts = new HashMap<String, WorkProduct>();
        
    }

    //@Transformer
    public Message<List<TGItem>> manageIncident(
        Message<List<TGItem>> messages) {

        ArrayList<TGItem> tgMessages = new ArrayList<TGItem>();
        HashMap<String, WorkProduct> needCloseIncidentWorkProducts = new HashMap<String, WorkProduct>();

        // Get the current work products for all the incidents on the core
        updateCurrentIncidentWorkProducts();

        // Try to match each incoming message with an incident currently on the core
        for (TGItem message : messages.getPayload()) {

            String storeno = message.getLocationNumber();

            WorkProductDocument incidentWorkProduct = lookForExistingIncident(storeno);

            if (incidentWorkProduct == null) {
                tgMessages.add(message);
            } else if (tgItemNeedsUpdate(message, incidentWorkProduct)) {
            	/** dsh needs to figure out how to update*/
                //message.setWorkProductDocument(incidentWorkProduct);
            	//**dsh close and archive the old one and create a new one?
            	
                tgMessages.add(message);
            }
          //**dsh Can we just close and archive right here?
            if (!currentTargetIncidentWorkProducts.containsKey(storeno)){
            	
            }

        }

        // Find any incidents that are missing from the incoming list and setup to
        // close and archive the incident       
        /*ArrayList<IncidentDocumentMessage> incidentsToArchive = getIncidentsToRemove(messages);
        if (incidentsToArchive.size() > 0) {
            wgMessages.addAll(incidentsToArchive);
        }*/
      

        return new GenericMessage<List<TGItem>>(tgMessages);
    }

    public boolean tgItemNeedsUpdate(TGItem message, WorkProductDocument incidentWorkProduct) {   	

        IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(incidentWorkProduct.getWorkProduct());
        if (incident.getIncident().sizeOfActivityDescriptionTextArray() > 0){
           String description = incident.getIncident().getActivityDescriptionTextArray(0).toString();
           String delims = "<br/><b>";
           String[] tokens = description.split(delims);
           String itemDelims = "</b>";
           /*check status*/
           String[] statusToken = tokens[1].split(itemDelims);
           logger.debug("Checking Status Token, token 0 =" + statusToken[0]);
           logger.debug("Status token 1 =" + statusToken[1]);
           if (statusToken[1].equalsIgnoreCase(message.getOperationStatus())){
        	   return true;
           }
           /*check Power Outage*/
           String[] powerToken = tokens[2].split(itemDelims);
           if (powerToken[1].equalsIgnoreCase(message.getPowerOutageStatus())){
        	   return true;
           }
           /*check Close Time*/
           String[] closeToken = tokens[3].split(itemDelims);
           if (closeToken[1].equalsIgnoreCase(message.getCloseTime())){
        	   return true;
           }
           /*check Open Time*/
           String[] openToken = tokens[4].split(itemDelims);
           if (openToken[1].equalsIgnoreCase(message.getOpenTime())){
        	   return true;
           }
           /*check Facility Damage*/
           String[] damageToken = tokens[5].split(itemDelims);
           if (damageToken[1].equalsIgnoreCase(message.getFacilityDamage()+"</br>")){
        	   return true;
           }
        }
        return false;
    }

    /*public ArrayList<IncidentDocumentMessage> getIncidentsToRemove(
        Message<List<IncidentDocumentMessage>> messages) {

        HashSet<String> set = new HashSet<String>();
        for (IncidentDocumentMessage message : messages.getPayload()) {
            XmlObject ids[] = message.getRtta().selectChildren("", "id");
            if (ids.length > 0) {
                set.add(Common.getTextFromAny(ids[0]));
            }
        }

        ArrayList<IncidentDocumentMessage> incidentsToArchive = new ArrayList<IncidentDocumentMessage>();
        for (String rttaID : currentDeldotIncidentWorkProducts.keySet()) {
            if (!set.contains(rttaID)) {
                IncidentDocumentMessage doc = new IncidentDocumentMessage();
                WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
                wpd.setWorkProduct(currentDeldotIncidentWorkProducts.get(rttaID));
                doc.setWorkProductDocument(wpd);
                incidentsToArchive.add(doc);
            }
        }
        return incidentsToArchive;
    }*/

    public void updateCurrentIncidentWorkProducts() {

        WorkProductList incidentList = getIncidentList();

        if (incidentList != null) {

            // only clear the list if we got a valid incident list
            currentTargetIncidentWorkProducts.clear();

            for (WorkProduct wp : incidentList.getWorkProductArray()) {
                IdentificationType id = Common.getIdentificationElement(wp);
                IncidentDocument incidentDoc = getIncidentDocument(id);
                if (incidentDoc != null && incidentDoc.getIncident() != null) {
                    String storeID = findStoreID(incidentDoc.getIncident());
                    if (storeID != null) {
                        if (wp.sizeOfStructuredPayloadArray() == 0) {
                            wp.addNewStructuredPayload();
                        }
                        wp.getStructuredPayloadArray(0).set(incidentDoc);
                        currentTargetIncidentWorkProducts.put(storeID, wp);
                    }
                }
            }

        }

    }

   public WorkProductDocument lookForExistingIncident(String storeNo) {

        
        // Found an incident that was created from this target item
        if (currentTargetIncidentWorkProducts.containsKey(storeNo)) {
            WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
            wpd.setWorkProduct(currentTargetIncidentWorkProducts.get(storeNo));
            return wpd;
        }

        return null;
    }

    private IncidentDocument getIncidentDocument(IdentificationType workProductIdentifier) {

        WorkProduct workProduct = uicdsCore.getWorkProductFromCore(workProductIdentifier);
        if (workProduct != null) {
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

    public static String findStoreID(UICDSIncidentType incident) {

        boolean foundTarget = false;
        String descriptionText;
        String delims = ":";
        String storeNo;
        
        if (incident != null && incident.sizeOfIncidentEventArray() > 0) {
            for (ActivityType event : incident.getIncidentEventArray()) {
                if (event.sizeOfActivityCategoryTextArray() > 0) {
                    for (TextType category : event.getActivityCategoryTextArray()) {
                        if (category.getStringValue().equalsIgnoreCase("Target Event")) {
                            foundTarget = true;
                        }
                    }
                }
                
                if (foundTarget) {
                    if (incident.sizeOfIncidentLocationArray() > 0) {
                        for (LocationType location : incident.getIncidentLocationArray()) {
                            if (location.sizeOfLocationAreaArray() > 0) {
                                if (location.getLocationAreaArray(0).sizeOfAreaCircularDescriptionTextArray() > 0) {
                                	  descriptionText = location.getLocationAreaArray(0).getAreaCircularDescriptionTextArray(0).getStringValue();
                                	  String[] tokens = descriptionText.split(delims); 
                                	  return tokens[1];
                                }
                                    
                                    }
                                }
                            }
                        }
                    }
                }
            
        return null;
    }

    public WorkProductList getIncidentList() {

        GetIncidentListRequestDocument request = GetIncidentListRequestDocument.Factory.newInstance();
        request.addNewGetIncidentListRequest();
        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        if (response instanceof GetIncidentListResponseDocument) {
            GetIncidentListResponseDocument incidentList = (GetIncidentListResponseDocument) response;
            if (incidentList != null) {
                return incidentList.getGetIncidentListResponse().getWorkProductList();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
