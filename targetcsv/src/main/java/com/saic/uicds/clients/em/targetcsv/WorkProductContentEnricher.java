package com.saic.uicds.clients.em.targetcsv;

import gov.niem.niem.niemCore.x20.ActivityDateDocument;
import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.DateTimeDocument;
import gov.niem.niem.niemCore.x20.DateType;
import gov.niem.niem.niemCore.x20.LengthMeasureType;
import gov.niem.niem.niemCore.x20.LengthUnitCodeDocument;
import gov.niem.niem.niemCore.x20.MeasurePointValueDocument;
import gov.niem.niem.niemCore.x20.TextType;
import gov.niem.niem.proxy.xsd.x20.DateTime;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;

import com.saic.uicds.clients.util.Common;

public class WorkProductContentEnricher {

    private static final String DEFAULT_EVENT_TYPE = "Target";

    private Logger logger = LoggerFactory.getLogger(this.getClass());
          
    private String tgStoreIndexString;
    
    
    @Transformer
    public Message<WorkProductDocument> createWorkProduct(Message<IncidentDocumentMessage> message) {
        logger.debug("In WorkProductContentEnricher.createWorkProduct From Message");        
        
        //Find the current incident document if it exists in the message headers
        WorkProductDocument workProductDoc = message.getPayload().getWorkProductDocument();
        
        if(Utilities.tgIncidentIsClosed(message.getPayload().getData())){
        	if (message.getPayload().getData() != null) {
        	logger.info("Closing UICDS incident for: "+ message.getPayload().getData().getLocationNumber());
        	} else {
                logger.info("Closing UICDS incident.");
            }
        	
        	if (message.getPayload().getWorkProductDocument() != null) {
                return MessageBuilder.withPayload(message.getPayload().getWorkProductDocument()).setHeaderIfAbsent(
                    "ARCHIVE", true).build();
            }
        } else
        	workProductDoc = getCreateOrUpdateWorkProduct(message, workProductDoc);

        return new GenericMessage<WorkProductDocument>(workProductDoc);
    }
    
    public WorkProductDocument getCreateOrUpdateWorkProduct(
            Message<IncidentDocumentMessage> message, WorkProductDocument workProductDoc) {

            IncidentDocument incident = null;

            // Create a new work product doc if one was not passed through the headers
            // this indicates that the incident for this item doesn't exist.
            if (workProductDoc == null) {
                incident = createIncidentFromTgItem(message.getPayload());
                workProductDoc = WorkProductDocument.Factory.newInstance();
                logger.info("Creating UICDS incident for :" + message.getPayload().getData().getLocationNumber());
            } else {
                incident = updateIncidentFromTgItem(message.getPayload(), workProductDoc);
                logger.info("Updating UICDS incident for :" + message.getPayload().getData().getLocationNumber());
            }

            if (workProductDoc.getWorkProduct() == null) {
                workProductDoc.addNewWorkProduct();
            }
            if (workProductDoc.getWorkProduct().sizeOfStructuredPayloadArray() == 0) {
                workProductDoc.getWorkProduct().addNewStructuredPayload();
            }
            workProductDoc.getWorkProduct().getStructuredPayloadArray(0).set(incident);

            return workProductDoc;
        }

    /*private WorkProductDocument createIncidentWorkProduct(WGItem item) {

        WorkProductDocument workProductDoc = WorkProductDocument.Factory.newInstance();

        IncidentDocument incident = createIncidentFromWGItem(item);

        workProductDoc.addNewWorkProduct();
        workProductDoc.getWorkProduct().addNewStructuredPayload();
        workProductDoc.getWorkProduct().getStructuredPayloadArray(0).set(incident);

        return workProductDoc;
    }*/

    private IncidentDocument updateIncidentFromTgItem(IncidentDocumentMessage payload,
            WorkProductDocument workProductDoc) {

            IncidentDocument incidentDoc = Common.getIncidentDocumentFromWorkProduct(workProductDoc.getWorkProduct());
            if (incidentDoc.getIncident() != null) {
                mapTGDataToIncident(payload, incidentDoc.getIncident());
            }
            return incidentDoc;
        }

    private void mapTGDataToIncident(IncidentDocumentMessage incidentDocumentMessage, UICDSIncidentType incident) {
        
        setActivityName(incident, "Target - "+ incidentDocumentMessage.getData().getCity()+" "+ incidentDocumentMessage.getData().getState());
        setActivityCategory(incident, DEFAULT_EVENT_TYPE);
        setActivityDate(incident, null, null);
        logger.debug("Store location Type: " + incidentDocumentMessage.getData().getLocationType());
        logger.debug("Store City: " + incidentDocumentMessage.getData().getCity());
        logger.debug("Store Address: " + incidentDocumentMessage.getData().getAddress());
        logger.debug("Store Zip: " + incidentDocumentMessage.getData().getZip());
        logger.debug("Store State: " + incidentDocumentMessage.getData().getState());
        logger.debug("Store Op Status: " + incidentDocumentMessage.getData().getOperationStatus());
        logger.debug("Store Pwr Status: " + incidentDocumentMessage.getData().getPowerOutageStatus());
        logger.debug("Store Close Time: " + incidentDocumentMessage.getData().getCloseTime());
        logger.debug("Store Open Time: " + incidentDocumentMessage.getData().getOpenTime());
        logger.debug("Store Facility Damage: " + incidentDocumentMessage.getData().getFacilityDamage());
        logger.debug("Store Latitude: " + incidentDocumentMessage.getData().getLatitude());
        logger.debug("Store Longitude: " + incidentDocumentMessage.getData().getLongitude());
        setStoreData(incident, incidentDocumentMessage.getData().getLocationType(), incidentDocumentMessage.getData().getLocationNumber(), incidentDocumentMessage.getData().getCity(),
        		 incidentDocumentMessage.getData().getAddress(), incidentDocumentMessage.getData().getZip(), incidentDocumentMessage.getData().getState(), 
        		 incidentDocumentMessage.getData().getOperationStatus(), incidentDocumentMessage.getData().getPowerOutageStatus(),
        		 incidentDocumentMessage.getData().getCloseTime(), incidentDocumentMessage.getData().getOpenTime(),
        		 incidentDocumentMessage.getData().getFacilityDamage(),incidentDocumentMessage.getData().getLatitude(),incidentDocumentMessage.getData().getLongitude());
        //setActivityDescription(incident, wgItem.getSiteNumber());
        
    }

    private IncidentDocument createIncidentFromTgItem(IncidentDocumentMessage incidentDocumentMessage) {
        logger.debug("Creating Incident From Target Store " + incidentDocumentMessage.getData().getLocationNumber());
        IncidentDocument incidentDoc = IncidentDocument.Factory.newInstance();
        incidentDoc.addNewIncident();     
        mapTGDataToIncident(incidentDocumentMessage, incidentDoc.getIncident());
        	
        return incidentDoc;
    }

    

 /*   public WGStoreObject findWGObject(String storeno)
    {
    	WGStoreObject obj =new WGStoreObject();
    	logger.debug("Looking for Store Number:"+storeno);
    	//logger.debug("WgList has: " + wgList.size());
    	Iterator<WGStoreObject> iterator = WGStoreAdapter.wgList.iterator();
		while (iterator.hasNext()) 
		{
			obj = iterator.next();
			//logger.debug("The current object store no: "+ obj.getStoreNbr());
			if(obj.getStoreNbr().equalsIgnoreCase(storeno))
			{
				return obj;
			}
		}
		
    	return null;
    	
    }*/
    
	private long getAdler32Checksum(String string) {

        byte bytes[] = string.getBytes();
        Checksum checksum = new Adler32();
        checksum.update(bytes, 0, bytes.length);
        return checksum.getValue();
    }

    private void setActivityName(UICDSIncidentType incident, String title) {

        if (incident.sizeOfActivityNameArray() == 0) {
            incident.addNewActivityName();
        }

        incident.getActivityNameArray(0).setStringValue(title);
    }

    private void setActivityCategory(UICDSIncidentType incident, String type) {

        if (incident.sizeOfActivityCategoryTextArray() == 0) {
            incident.addNewActivityCategoryText();
        }
        incident.getActivityCategoryTextArray(0).setStringValue(type);
    }

    // TODO: should be put in a common utilities class
    private static String getNowAsString() {

        Calendar cal = Calendar.getInstance();
        //SimpleDateFormat ISO8601Local = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        ISO8601Local.setTimeZone(timeZone);
        return ISO8601Local.format(cal.getTime());
    }

    private static Date getDateFromWebEOCEntrydataField(String dateTimeString)
        throws ParseException {

        Date dateTime = null;
        if (dateTimeString != null && dateTimeString.length() > 0) {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("dd-MMM-yy HHmm z");
            TimeZone timeZone = TimeZone.getDefault();
            ISO8601Local.setTimeZone(timeZone);
            dateTime = (Date) ISO8601Local.parse(dateTimeString.trim());
        }
        return dateTime;
    }

    private void setActivityDate(UICDSIncidentType incident, String date, String time) {

        String dateString = null;

        if (date == null || time == null) {
        	//logger.debug("***GetNowAsString");
            dateString = getNowAsString();
        } else {
            DateTime dt = DateTime.Factory.newInstance();
            try {
                Date ds = getDateFromWebEOCEntrydataField(date + " " + time);
                dt.setDateValue(ds);
            } catch (ParseException e) {
                logger.error("Error parsing date " + date + " " + time + " : " + e.getMessage());
                return;
            }
            // dt.setDateValue();
            dateString = Common.getTextFromAny(dt);
        }

        DateTimeDocument dateDoc = DateTimeDocument.Factory.newInstance();
        dateDoc.addNewDateTime().setStringValue(dateString);

        ActivityDateDocument activityDate = ActivityDateDocument.Factory.newInstance();
        activityDate.addNewActivityDate().set(dateDoc);

        if (incident.sizeOfActivityDateRepresentationArray() < 1) {
            Common.substitute(incident.addNewActivityDateRepresentation(), Common.NIEM_NS,
                Common.ACTIVITY_DATE, DateType.type, activityDate.getActivityDate());
        } else {
            incident.getActivityDateRepresentationArray(0).set(activityDate.getActivityDate());
        }
    }

    private void setStoreData(UICDSIncidentType incident, String locationType , String locationNumber, String city, String address, String zip, String state,
    		          String operationStatus, String powerOutageStatus, String closeTime, String openTime, 
    		          String facilityDamage, String latitude, String longitude ) {
    	 
    	//WGStoreObject obj = findWGObject(storeno);
    	//if(obj !=null)
 	   //{
    		//Set the location
    	double tglat = 0.0;
    	double tglong = 0.0;
    	
    	if (latitude != null && !latitude.isEmpty()){
 		   tglat  = Double.valueOf(latitude);
    	}
    	if (longitude != null && !longitude.isEmpty()){
 		   tglong = Double.valueOf(longitude);
    	}
 		  if (incident.sizeOfIncidentLocationArray() == 0) {
              incident.addNewIncidentLocation();
              logger.debug("Added Incident Location, array = "+incident.sizeOfIncidentLocationArray() );
          }
 		  if (incident.getIncidentLocationArray(0).sizeOfLocationAreaArray() == 0){
 		     AreaType area = incident.getIncidentLocationArray(0).addNewLocationArea();
 	         area.addNewAreaCircularDescriptionText().setStringValue("Target Location No:"+locationNumber);
 	         area.addNewAreaCircularRegion().set(
 	            Common.createCircle((new Double(tglat).toString()), (new Double(tglong).toString())));
 	         LengthMeasureType radius = area.getAreaCircularRegionArray(0).addNewCircularRegionRadiusLengthMeasure();
 	         MeasurePointValueDocument value = MeasurePointValueDocument.Factory.newInstance();
 	         value.addNewMeasurePointValue().setStringValue("4.5");
 	         LengthUnitCodeDocument lc = LengthUnitCodeDocument.Factory.newInstance();
 	         lc.addNewLengthUnitCode().setStringValue("SMI");
 	         radius.set(value);
 	         radius.set(lc);
 		  }
 	      
 	       //Set the Activity Description Text
 	      if (incident.sizeOfActivityDescriptionTextArray() == 0) {
 	       incident.addNewActivityDescriptionText();
 	      }
 	      TextType description = incident.getActivityDescriptionTextArray(0);
 	      //description.setStringValue("<![CDATA[<br/><b>Store/Site Status: </b>" + status +
 	      description.setStringValue("<br/><b>Store/Site Status: </b>" + operationStatus +
 	    		    "<br/><b>Power Outage Status: </b>" + powerOutageStatus +
 	    		    "<br/><b>Planned/Actual Close Time: </b>" + closeTime +
 	    		    "<br/><b>Planned/Actual Open Time: </b>" + openTime +
 	    		    "<br/><b>Facility Damage: </b>" + facilityDamage +
 	    		    "<br/><br/><b>STORE DETAILS</b>" +
 	    		    "<br/><b>Address: </b>" + address + "," + city + "," + state + "," + zip +
 	    		    "<br/><b>Location Type: </b>" + locationType +
 	    		    "<br/><b>Location Number: </b>" + locationNumber + "<br/>");
   
 	    		    //"<br/><b>Operation: </b>" + obj.getOperationNbr() + "<br/>]]>");
 	  /* } else {
 		   //If cannot match the store number, set the location to 0.0 0.0
 		
 		   double wglat  = Double.valueOf(0.0);
 		   double wglong = Double.valueOf(0.0);
 		  if (incident.sizeOfIncidentLocationArray() == 0) {
              incident.addNewIncidentLocation();
              logger.debug("Added Incident Location, array = "+incident.sizeOfIncidentLocationArray() );
          }
 		  if (incident.getIncidentLocationArray(0).sizeOfLocationAreaArray() == 0){
 		     AreaType area = incident.getIncidentLocationArray(0).addNewLocationArea();
 	         area.addNewAreaCircularDescriptionText().setStringValue("Walgreens Store No:"+storeno);
 	         area.addNewAreaCircularRegion().set(
 	            Common.createCircle((new Double(wglat).toString()), (new Double(wglong).toString())));
 	         LengthMeasureType radius = area.getAreaCircularRegionArray(0).addNewCircularRegionRadiusLengthMeasure();
 	         MeasurePointValueDocument value = MeasurePointValueDocument.Factory.newInstance();
 	         value.addNewMeasurePointValue().setStringValue("4.5");
 	         LengthUnitCodeDocument lc = LengthUnitCodeDocument.Factory.newInstance();
 	         lc.addNewLengthUnitCode().setStringValue("SMI");
 	         radius.set(value);
 	         radius.set(lc);
 		  }
 		  
 		 //Set the Activity Description Text without Store data
 	      if (incident.sizeOfActivityDescriptionTextArray() == 0) {
 	       incident.addNewActivityDescriptionText();
 	      }
 	      TextType description = incident.getActivityDescriptionTextArray(0);
 	      //description.setStringValue("<![CDATA[<br/><b>Store/Site Status: </b>" + status +
 	      description.setStringValue("<br/><b>Store/Site Status: </b>" + status +
 	    		    "<br/><b>Store Priroity: </b>" + priority +
 	    		    "<br/><b>Current Power Status: </b>" + pwrstatus +
 	    		    "<br/><b>Generator Deployed: </b>" + gendeployed +
 	    		    "<br/><b>Damage Level: </b>" + damage + 
 	    		    "<br/><br/><b>STORE DETAILS</b>" +
 	    		    "<br/><b>Address: </b>" + "Incorrect Store No" +
	    		    "<br/><b>Store Type: </b>" + "Incorrect Store No" +
	    		    "<br/><b>Retail Clinic: </b>" + "Incorrect Store No" +
	    		    "<br/><b>Open 24 Hrs: </b>" + "Incorrect Store No" +
	    		    "<br/><b>Store Number: </b>" + storeno +
	    		    "<br/><b>NCPDP Provider ID: </b>" + "Incorrect Store No" +
	    		    "<br/><b>District: </b>" + "Incorrect Store No"  +
	    		    "<br/><b>Region: </b>" + "Incorrect Store No" +
	    		    "<br/><b>Operation: </b>" + "Incorrect Store No" + "<br/>");*/
 	    		    
 	  // }
 		   
        /*try {
            // CircularRegionType circle = Common.createCircle(latitude, longitude);
            CircularRegionType circle = CircularRegionType.Factory.newInstance();

            TwoDimensionalGeographicCoordinateType center = TwoDimensionalGeographicCoordinateType.Factory.newInstance();

            LatitudeCoordinateType latCoord = LatitudeCoordinateType.Factory.newInstance();
            latCoord.addNewLatitudeDegreeValue().setStringValue(latDeg);
            latCoord.addNewLatitudeMinuteValue().setStringValue(latMin);
            latCoord.addNewLatitudeSecondValue().setStringValue(latSec);
            center.setGeographicCoordinateLatitude(latCoord);

            LongitudeCoordinateType lonCoord = LongitudeCoordinateType.Factory.newInstance();
            lonCoord.addNewLongitudeDegreeValue().setStringValue(lonDeg);
            lonCoord.addNewLongitudeMinuteValue().setStringValue(lonMin);
            lonCoord.addNewLongitudeSecondValue().setStringValue(lonSec);
            center.setGeographicCoordinateLongitude(lonCoord);

            circle.addNewCircularRegionCenterCoordinate().set(center);

            LengthMeasureType radius = circle.addNewCircularRegionRadiusLengthMeasure();
            MeasurePointValueDocument value = MeasurePointValueDocument.Factory.newInstance();
            value.addNewMeasurePointValue().setStringValue("0.0");
            radius.set(value);

            if (incident.sizeOfIncidentLocationArray() == 0) {
                incident.addNewIncidentLocation();
            }
            if (incident.getIncidentLocationArray(0).sizeOfLocationAreaArray() < 1) {
                incident.getIncidentLocationArray(0).addNewLocationArea();
            }
            if (incident.getIncidentLocationArray(0).getLocationAreaArray(0).sizeOfAreaCircularRegionArray() < 1) {
                incident.getIncidentLocationArray(0).getLocationAreaArray(0).addNewAreaCircularRegion();
            }

            incident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(
                0).set(circle);

        } catch (StringIndexOutOfBoundsException e) {
            logger.error("Error creating circle location from (" + latitude + "," + longitude);
        }*/

    }
    


       


    private void setActivityDescription(UICDSIncidentType incident, String description) {

        if (incident.sizeOfActivityDescriptionTextArray() == 0) {
            incident.addNewActivityDescriptionText();
        }
        incident.getActivityDescriptionTextArray(0).setStringValue(description);
    }

	public String getTgStoreIndexString() {
		return tgStoreIndexString;
	}

	public void setTgStoreIndexString(String tgStoreIndexString) {
		this.tgStoreIndexString = tgStoreIndexString;
	}
}
