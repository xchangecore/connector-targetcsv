package com.saic.uicds.clients.em.targetcsv;

import java.util.ArrayList;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;

public class IncidentDocumentMessage {

    private TGItem data;

    private ArrayList<TGItem> events;

    private WorkProductDocument workProductDocument;

    public IncidentDocumentMessage() {

        events = new ArrayList<TGItem>();
    }

    /**
     * @return the data
     */
    public TGItem getData() {

        return data;
    }

    /**
     * @param data - the data to set
     */
    public void setTgdata(TGItem data) {

        this.data = data;
    }

    public void addEvent(TGItem event) {

        events.add(event);
    }

    public ArrayList<TGItem> getEvents() {

        return events;
    }

    /**
     * @return the workProductDocument
     */
    public WorkProductDocument getWorkProductDocument() {

        return workProductDocument;
    }

    /**
     * @param workProductDocument the workProductDocument to set
     */
    public void setWorkProductDocument(WorkProductDocument workProductDocument) {

        this.workProductDocument = workProductDocument;
    }

}
