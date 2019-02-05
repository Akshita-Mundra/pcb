package com.sc.fe.analyze.util;

import com.datastax.driver.core.utils.UUIDs;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sc.fe.analyze.data.entity.Report;
import com.sc.fe.analyze.to.AdvancedReport;
import com.sc.fe.analyze.to.CustomerInformation;
import com.sc.fe.analyze.data.entity.Project;
import com.sc.fe.analyze.data.entity.ProjectFiles;
import com.sc.fe.analyze.to.ProjectDetails;
import com.sc.fe.analyze.to.ProjectFilesDetails;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Hemant
 */
public class ReportUtility {

	private ReportUtility() {};
	
    /**
     * This method is convert the report into object.
     * @param dbData - report of any object
     * @return the report
     */
    public static com.sc.fe.analyze.to.Report convertToObject(Report dbData) {
		
		com.sc.fe.analyze.to.Report report = new com.sc.fe.analyze.to.Report();
		
		report.setErrors(dbData.getErrors());
		
		Map<String, Set<String>> filePurposeToNameMapping = new HashMap<String, Set<String>>();
		
		Map<Integer, String> dbMap = dbData.getFileTypeToFileNameMapping();
		Iterator<Integer> keyIter = dbMap.keySet().iterator();
		
		while( keyIter.hasNext()) {
			Integer iKey = keyIter.next();
			String key = iKey.toString();
			String value = dbMap.get(iKey);
			if( filePurposeToNameMapping.containsKey(key) ) {
				filePurposeToNameMapping.get(key).add(value);
			}else {
				Set<String> set = new HashSet<String>();
				set.add(value);
				filePurposeToNameMapping.put(key, set);
			}
		}
		
		report.setSummary(dbData.getSummary());
		report.setValidationStatus(dbData.getStatus());
		
		CustomerInformation customerInputs = new CustomerInformation();
		customerInputs.setProjectId(dbData.getProjectId());
		customerInputs.setCustomerId(dbData.getCustomerId());
		//customerInputs.setServiceType(MappingUtil.getServiceName(dbData.getServiceTypeId()) );
		
		//report.setCustomerInputs(customerInputs );
		
		return report;
	}
	
    /**
     * This method is convert the AdvancedReport object into database object.
     * @param report - object of AdvancedReport
     * @return the report
     */
    public static Report convertToDBObject(com.sc.fe.analyze.to.Report report) {
		
		Report dbReport = new Report();
		
//		CustomerInformation customerInputs = report.getProjectDetail().getCustomerInformation();
//		dbReport.setCustomerId( customerInputs.getCustomerId() );
//		dbReport.setProjectId( customerInputs.getProjectId() );
		//dbReport.setServiceTypeId(MappingUtil.getServiceId(customerInputs.getServiceType()));
		
		dbReport.setErrors(report.getErrors());
		
		Map<Integer, String> dbMap = new HashMap<Integer, String> ();
		//Map<String, Set<String>> sourceMap = report.getFilePurposeToNameMapping();
		
//		Iterator<String> keyIter = sourceMap.keySet().iterator();
//		
//		while( keyIter.hasNext()) {
//			String key = keyIter.next();
//			Set<String> values = sourceMap.get(key);
//			values.stream().forEach( e -> { ;
//				dbMap.put(MappingUtil.getFileTypeId(key), e );
//			});
//		}
		
		dbReport.setFileTypeToFileNameMapping(dbMap);
		
		//dbReport.setProjectFiles(report.getExctractedFileNames());
		dbReport.setStatus(report.getValidationStatus());
		dbReport.setSummary(report.getSummary());
		dbReport.setVersion(1);
		
		return dbReport;
	}
        public static Project convertToDBObject(ProjectDetails projectDetails)
        {
            Project dbDetail=new Project();
            dbDetail.setCustomerEmail(projectDetails.getEmailAddress());
            dbDetail.setCustomerId(projectDetails.getCustomerId());
            dbDetail.setDesignSpecification(projectDetails.getDesignSpecification());
            dbDetail.setErrors(projectDetails.getErrors());
            dbDetail.setPcbClass(projectDetails.getPcbClass());
            dbDetail.setProjectId(projectDetails.getProjectId());
            dbDetail.setVersion(UUIDs.timeBased());            
            
            dbDetail.setTurnTimeQuantity(projectDetails.getTurnTimeQuantity());
            dbDetail.setServiceType(projectDetails.getServiceType());
            dbDetail.setZipFileName(projectDetails.getZipFileName());
            dbDetail.setZipFileSize(projectDetails.getZipFileSize());
            dbDetail.setLayerCount(projectDetails.getLayers());
            dbDetail.setModifiedDate(new Date());
            dbDetail.setCreateDate(new Date());
            dbDetail.setCustomerName("ABC");                 
            
            return dbDetail;
        }
	public static ProjectFiles convertToDBObject(ProjectFilesDetails projectFileDetails){
            ProjectFiles projectFilesDbDetails=new ProjectFiles();
            projectFilesDbDetails.setProjectId(projectFileDetails.getProjectId());
            projectFilesDbDetails.setVersion(UUIDs.timeBased());
            projectFilesDbDetails.setName(projectFileDetails.getName());
            projectFilesDbDetails.setSize(projectFileDetails.getSize());
            projectFilesDbDetails.setFileDate(new Date());
            projectFilesDbDetails.setFormat(projectFileDetails.getFormat());
            projectFilesDbDetails.setStep(projectFileDetails.getStep());
            projectFilesDbDetails.setContext(projectFileDetails.getContext());
            projectFilesDbDetails.setPolarity(projectFileDetails.getPolarity());
            projectFilesDbDetails.setSide(projectFileDetails.getSide());
            projectFilesDbDetails.setLayerOrder(projectFileDetails.getLayerOrder());
            projectFilesDbDetails.setStartName(projectFileDetails.getStartName());
            projectFilesDbDetails.setEndName(projectFileDetails.getEndName());
            projectFilesDbDetails.setCopperWeight(projectFileDetails.getCopperWeight());
            projectFilesDbDetails.setLayerName(projectFileDetails.getLayerName());
            projectFilesDbDetails.setAttributes(projectFileDetails.getAttributes());
            projectFilesDbDetails.setCreateDate(projectFileDetails.getCreateDate());
            projectFilesDbDetails.setModifiedDate(projectFileDetails.getModifiedDate());
            projectFilesDbDetails.setErrors(projectFileDetails.getErrors());
            return projectFilesDbDetails;
        } 

}
