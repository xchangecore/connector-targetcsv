/**
 * 
 */
package com.saic.uicds.clients.em.targetcsv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.validation.BindException;

/**
 * @author dsh import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
 * 
 * 
 *         public class BlankLineRecordSeparatorPolicy extends SimpleRecordSeparatorPolicy {
 * @Override public boolean isEndOfRecord(String aLine) { if (aLine.trim().length() ==0) { return
 *           false; } return super.isEndOfRecord(aLine); }
 * @Override public String postProcess(String aRecord) { if (aRecord==null ||
 *           aRecord.trim().length()==0 || "null".equals(aRecord)) { return null; } return
 *           super.postProcess(aRecord); } }
 */
public class TGFileAdapter
    implements FieldSetMapper<TGItem> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean deleteFile = true;

    private FlatFileItemReader<TGItem> reader;
    
    IncidentManager incidentMgr = new IncidentManager();

    /**
     * @return the reader
     */
    public FlatFileItemReader<TGItem> getReader() {

        return reader;
    }

    /**
     * @param reader the reader to set
     */
    public void setReader(FlatFileItemReader<TGItem> reader) {

        this.reader = reader;
    }

    public boolean isDeleteFile() {

        return deleteFile;
    }

    public void setDeleteFile(boolean deleteFile) {

        this.deleteFile = deleteFile;
    }

    public void initialize() {

        reader = new FlatFileItemReader<TGItem>();
        reader.setRecordSeparatorPolicy(new SimpleRecordSeparatorPolicy());

        DefaultLineMapper<TGItem> lineMapper = new DefaultLineMapper<TGItem>();
        lineMapper.setFieldSetMapper(new TGFileAdapter());

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(',');
        tokenizer.setQuoteCharacter('"');
        tokenizer.setNames(TGItem.fieldNames);
        lineMapper.setLineTokenizer(tokenizer);

        reader.setLineMapper(lineMapper);
        reader.setLinesToSkip(1);
    }

    @ServiceActivator
    public List<TGItem> handleFile(Message<File> message) {

    	File file = message.getPayload();

        logger.debug("Reading file: " + file.getAbsolutePath());

        FileSystemResource fileResource = new FileSystemResource(file);
        reader.setResource(fileResource);
        ExecutionContext executionContext = new ExecutionContext();
        reader.open(executionContext);

        ArrayList<TGItem> tgItems = new ArrayList<TGItem>();

        boolean blankLine = false;
        TGItem item = null;
        try {
            item = reader.read();
        } catch (UnexpectedInputException e) {
            logger.error("Unexpected input: " + e.getMessage());
        } catch (ParseException e) {
            if (e.getCause() instanceof org.springframework.batch.item.file.transform.IncorrectTokenCountException) {
                org.springframework.batch.item.file.transform.IncorrectTokenCountException tce = (org.springframework.batch.item.file.transform.IncorrectTokenCountException) e.getCause();
                if (tce.getActualCount() == 0) {
                    logger.warn("ignoring empty line in input file");
                    blankLine = true;
                } else {
                    logger.error("Incorrect number of tokents on line. Expected "
                        + tce.getExpectedCount() + " but got " + tce.getActualCount());
                }
            } else {
                logger.error("Parser exception: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Reader exception: " + e.getMessage());
        }

        if (item != null || blankLine) {
            blankLine = false;
            if (item != null) {
                tgItems.add(item);
            }
            while (item != null || blankLine) {
                blankLine = false;
                try {
                    // an exception from the first reader.read will leave item pointing to the next
                    // entry so make sure not to add duplicates
                    item = null;
                    item = reader.read();
                    if (item != null) {
                        tgItems.add(item);
                    }
                } catch (UnexpectedInputException e) {
                    logger.error("Unexpected input: " + e.getMessage());
                } catch (ParseException e) {
                    if (e.getCause() instanceof org.springframework.batch.item.file.transform.IncorrectTokenCountException) {
                        org.springframework.batch.item.file.transform.IncorrectTokenCountException tce = (org.springframework.batch.item.file.transform.IncorrectTokenCountException) e.getCause();
                        if (tce.getActualCount() == 0) {
                            logger.warn("ignoring empty line in input file");
                            blankLine = true;
                        } else {
                            logger.error("Incorrect number of tokents on line. Expected "
                                + tce.getExpectedCount() + " but got " + tce.getActualCount());
                        }
                    } else {
                        logger.error("Parser exception: " + e.getMessage());
                    }
                } catch (Exception e) {
                    logger.error("Reader exception: " + e.getMessage());
                }
            }
        }

        reader.close();
        if (deleteFile) {
            if (file.delete()) {
                logger.info("File deleted");
            }
        }

        if (logger.isDebugEnabled() && tgItems.size() > 0) {
            for (TGItem tgItem : tgItems) {
                logger.debug("Found Incident: " + tgItem.getLocationNumber());
            }
        }
        return tgItems;
    }

    @Override
    public TGItem mapFieldSet(FieldSet fieldSet) throws BindException {

        if (fieldSet == null) {
            return null;
        }

        TGItem item = new TGItem();

        item.setLocationType(fieldSet.readString(TGItem.LOCATION_TYPE));
        item.setLocationNumber(fieldSet.readString(TGItem.LOCATION_NUMBER));
        item.setCity(fieldSet.readString(TGItem.CITY));
        item.setAddress(fieldSet.readString(TGItem.ADDRESS));
        item.setZip(fieldSet.readString(TGItem.ZIP));
        item.setState(fieldSet.readString(TGItem.STATE));
        item.setOperationStatus(fieldSet.readString(TGItem.OPERATION_STATUS));
        item.setPowerOutageStatus(fieldSet.readString(TGItem.POWER_OUTAGE_STATUS));
        item.setCloseTime(fieldSet.readString(TGItem.CLOSE_TIME));
        item.setOpenTime(fieldSet.readString(TGItem.OPEN_TIME));
        item.setFacilityDamage(fieldSet.readString(TGItem.FACILITY_DAMAGE));
        item.setLatitude(fieldSet.readString(TGItem.LATITUDE));
        item.setLongitude(fieldSet.readString(TGItem.LONGITUDE));

        return item;
    }
}
