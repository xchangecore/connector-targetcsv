/**
 * 
 */
package com.saic.uicds.clients.em.targetcsv;

import java.io.Serializable;

/**
 * @author dsh
 * 
 */
public class TGItem
    implements Serializable {

 
    /**
	 * 
	 */
	private static final long serialVersionUID = -8293683854224644705L;

	/**
     * 
     */

    public static final String LOCATION_TYPE = "Location Type";

    public static final String LOCATION_NUMBER = "Location Number";

    public static final String CITY = "City";

    public static final String ADDRESS = "Address";

    public static final String ZIP = "Zip";
    
    public static final String STATE = "State";

    public static final String OPERATION_STATUS = "Operation Status";

    public static final String POWER_OUTAGE_STATUS = "Power Outage Status";
    
    public static final String CLOSE_TIME = "Planned/Actual Close Time";
    
    public static final String OPEN_TIME = "Planned/Actual Open Time";
    
    public static final String FACILITY_DAMAGE = "Facility Damage";
    
    public static final String LATITUDE = "latitude";
    
    public static final String LONGITUDE = "longitude";

    /**
     * 
     */

    private String locationType;
    private String locationNumber;
    private String city;
    private String address;
    private String zip;
    private String state;
    private String operationStatus;
    private String powerOutageStatus;
    private String closeTime;
    private String openTime;
    private String facilityDamage;
    private String latitude;
    private String longitude;

    static public final String[] fieldNames = {
        LOCATION_TYPE,
        LOCATION_NUMBER,
        CITY,
        ADDRESS,
        ZIP,
        STATE,
        OPERATION_STATUS,
        POWER_OUTAGE_STATUS,
        CLOSE_TIME,
        OPEN_TIME,
        FACILITY_DAMAGE,
        LATITUDE,
        LONGITUDE};

	public String getLocationType() {
		return locationType;
	}

	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	public String getLocationNumber() {
		return locationNumber;
	}

	public void setLocationNumber(String locationNumber) {
		this.locationNumber = locationNumber;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getOperationStatus() {
		return operationStatus;
	}

	public void setOperationStatus(String operationStatus) {
		this.operationStatus = operationStatus;
	}

	public String getPowerOutageStatus() {
		return powerOutageStatus;
	}

	public void setPowerOutageStatus(String powerOutageStatus) {
		this.powerOutageStatus = powerOutageStatus;
	}

	public String getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(String closeTime) {
		this.closeTime = closeTime;
	}

	public String getOpenTime() {
		return openTime;
	}

	public void setOpenTime(String openTime) {
		this.openTime = openTime;
	}

	public String getFacilityDamage() {
		return facilityDamage;
	}

	public void setFacilityDamage(String facilityDamage) {
		this.facilityDamage = facilityDamage;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
}
