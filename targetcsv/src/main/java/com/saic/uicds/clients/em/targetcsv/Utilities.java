package com.saic.uicds.clients.em.targetcsv;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.IdentificationType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/*import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.uicds.incident.UICDSIncidentType;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;*/
import com.saic.uicds.clients.util.Common;

public class Utilities {

    /*public static ActivityType getWgIncidentEvent(UICDSIncidentType incident) {

        return Common.getIncidentEventByCategoryAndReason(incident, ,
            Constants.DC_CREATED_REASON);

    }*/

    public static String getDCNum_1FromActivityType(ActivityType event) {

        if (event.sizeOfActivityIdentificationArray() > 0) {
            for (IdentificationType identification : event.getActivityIdentificationArray()) {
                if (identification.sizeOfIdentificationIDArray() > 0) {
                    return identification.getIdentificationIDArray(0).getStringValue();
                }
            }
        }
        return null;
    }

    /*public static DirectPosition convertFromStatePlane(String x, String y) {

        Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        String fromEPSG = "EPSG:2248";
        String toEPSG = "EPSG:4326";

        Double xd = new Double(x) / 1000;
        Double yd = new Double(y) / 1000;

        xd *= 3.2808399;
        yd *= 3.2808399;

        DirectPosition geoPos = null;
        try {
            CoordinateReferenceSystem toCRS = CRS.decode(toEPSG);
            try {
                CoordinateReferenceSystem fromCRS = CRS.decode(fromEPSG);
                MathTransform math = CRS.findMathTransform(fromCRS, toCRS);
                DirectPosition pos = new GeneralDirectPosition(xd, yd);
                try {
                    geoPos = math.transform(pos, null);
                } catch (MismatchedDimensionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TransformException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (NoSuchAuthorityCodeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FactoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (NoSuchAuthorityCodeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // System.out.println("Converted " + xd + "," + yd + " to: " + geoPos.toString());
        return geoPos;
    }*/

    public static Date getDateFromDCItemTimestamp(String dateTimeStamp) {

        Date dateTime = null;
        if (dateTimeStamp != null && dateTimeStamp.length() > 0) {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyyMMddHHmmss");
            TimeZone timeZone = TimeZone.getDefault();
            ISO8601Local.setTimeZone(timeZone);
            try {
                dateTime = (Date) ISO8601Local.parse(dateTimeStamp.trim());
            } catch (ParseException e) {
                System.err.println(dateTimeStamp);
                System.err.println("Error parsing date string should be yyyyMMddHHmmss format: "
                    + e.getMessage());
            }
        }
        return dateTime;
    }

    public static String getNowAsString() {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat ISO8601Local = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        ISO8601Local.setTimeZone(timeZone);
        return ISO8601Local.format(cal.getTime());
    }

    /*public static List<DCIcadItem> getMostRecentEvents(List<DCIcadItem> events) {

        // sort the events into CDTS timestamp order (newest are first in list)
        Ordering<DCIcadItem> order = Ordering.natural().reverse().onResultOf(
            new Function<DCIcadItem, Date>() {
                public Date apply(DCIcadItem object) {

                    // CDTS may be null or empty when the incident has not been dispatched yet.
                    if (object.getCdts() != null && !object.getCdts().isEmpty()) {
                        return getDateFromDCItemTimestamp(object.getCdts());
                    } else {
                        return getDateFromDCItemTimestamp(object.getAd_ts());
                    }
                }
            });

        Collections.sort(events, order);

        // remove all items that are not in the latest CDTS set
        List<DCIcadItem> itemsToRemove = new ArrayList<DCIcadItem>();
        if (events.size() > 0) {
            String firstItemCdts = events.get(0).getCdts();
            for (DCIcadItem item : events) {
                if (!item.getCdts().equals(firstItemCdts)) {
                    itemsToRemove.add(item);
                }
            }
        }

        events.removeAll(itemsToRemove);

        return events;
    }*/

    static public boolean tgIncidentIsClosed(TGItem item) {

        if (item == null) {
            return true;
        }
        //**dsh 
        //return (item.getXdts() != null && !item.getXdts().isEmpty());
        return false;
    }

}
