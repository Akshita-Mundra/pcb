package com.sc.fe.analyze.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.utils.UUIDs;
import com.sc.fe.analyze.FileStorageProperties;
import com.sc.fe.analyze.data.entity.DifferenceReport;
import com.sc.fe.analyze.data.entity.ProjectFiles;
import com.sc.fe.analyze.to.FileDetails;
import com.sc.fe.analyze.to.ProjectDetails;
import com.sc.fe.analyze.to.Report;
import com.sc.fe.analyze.util.CompareUtility;
import com.sc.fe.analyze.util.ErrorCodeMap;
import com.sc.fe.analyze.util.FileStoreUtil;
import com.sc.fe.analyze.util.GerberFileProcessingUtil;
import com.sc.fe.analyze.util.MappingUtil;
import com.sc.fe.analyze.util.ODBProcessing;
import com.sc.fe.analyze.util.ReportUtility;

/**
 *
 * @author Hemant
 */
@Service
public class FileExtractUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileExtractUploadService.class);
    private FileStoreUtil util;

    //private S3FileUtility util;
    @Autowired
    BaseService baseService;
    @Autowired
    ProjectFilesService projectFilesService;
    @Autowired
    ProjectService projectService;

    /**
     *
     * @param fileStorageProperties property file containing file upload options
     */
    @Autowired
    public FileExtractUploadService(FileStorageProperties fileStorageProperties) {
        this.util = FileStoreUtil.getInstance(fileStorageProperties); //For local file
        //this.util = S3FileUtility.getInstance(fileStorageProperties); 	
    }

    /**
     * Validates the project files and send a report.
     *
     * @param projectDetails Details of the project
     * @return the report
     */
    public Report validateFiles(ProjectDetails projectDetails) {
        List<FileDetails> fileDetails = projectDetails.getFileDetails();
        //If no file details,  try to create it
        if (fileDetails == null || fileDetails.size() <= 0) {
            Set<String> allFiles = util.listFiles(projectDetails.getProjectId());
            allFiles.stream().forEach(name -> {
                FileDetails fd = new FileDetails();
                fd.setName(name);
                fileDetails.add(fd);
            });
        }

        // REPORT
        Report report = new Report();
        report.setProjectDetail(projectDetails);
        report.setSummary("****** File upload and basic validation by name and extension. *******");

        if( StringUtils.isEmpty(projectDetails.getServiceType()) ) {
    		projectDetails.getErrors().put("V0000", "Service Type is required");
    		return report;
    	} else {
	    	String[] splitServiceTypes = projectDetails.getServiceType().split(",");
	        for (int i = 0; i < splitServiceTypes.length; i++) {
	            String splitServiceType = splitServiceTypes[i].substring(0, 1).toUpperCase() + splitServiceTypes[i].substring(1, splitServiceTypes[i].length()).toLowerCase();
	            if (MappingUtil.getServiceId(splitServiceType) == null) {
	                projectDetails.getErrors().put("V0000", "Invalid Service Type - " + splitServiceTypes[i]);
	                return report;
	            }
	        }
    	}

        //GoldenCheck
        List<String> missingTypes = validateGoldenCheckRules(projectDetails);
        if (missingTypes != null) {
            if (missingTypes.size() > 0) {
                report.setValidationStatus("We found some missing information. ");
                missingTypes.stream().forEach(type -> {
                    if (!StringUtils.isEmpty(type)) {
                        report.addError(type);
                        report.addErrorCode(ErrorCodeMap.getCodeForFileType(type));
                    }
                });
            } else {
                report.setValidationStatus("Matched with all required file types. All information collected.");
            }
        }

        //Set errors
        Map<String, String> errMap = new HashMap<String, String>();
        if (report != null && report.getErrorCodes() != null) {

            report.getErrorCodes().stream().forEach(err -> {
                errMap.put(err.toString(), err.getErrorMessage());
            });
        }
        projectDetails.setErrors(errMap);

        //Save
        projectService.save(ReportUtility.convertToDBObject(projectDetails));

        //compare the last ProjectDetails 
        Map<String, String> compareMap = compareWithLastProjectData(projectDetails);
        String prevProjVersion = compareMap.get("version");
        compareMap.remove("version");

        //TODO: save the compare results in another table
        //Only store comparison of latest set. table key = ProjectId. 
        //Also need to store the value of last version which was compared as non key column
        //Errors will be formated text. Add these to report.error field
        projectDetails.setDifferences(CompareUtility.formatedError(compareMap));

        //Save the comparison Details
        if (!projectDetails.getDifferences().isEmpty()) {
            DifferenceReport diffReport = new DifferenceReport();
            diffReport.setProjectId(projectDetails.getProjectId());
            diffReport.setVersion(UUID.fromString(prevProjVersion));
            diffReport.setDifferences(projectDetails.getDifferences());
            projectService.save(diffReport);
        }

        return report;
    }

    /**
     * This method compares the projectDetails object from the last
     * projectDetails object from the database
     *
     * @param projectDetails Details of the project
     * @return the differences after comparing the latest project record from
     * the last project record
     */
    private Map<String, String> compareWithLastProjectData(ProjectDetails projectDetails) {

        Map<String, String> retErrors = new HashMap<String, String>();

        //Retrieve latest Record of similar project Id
        ProjectDetails prevprojDtl = getPreviousRecord(projectDetails);

        if (prevprojDtl != null) {
            //Retrieve attribute of ProjectDetails and FileDetails object of latest Record(from the database) and current Record.
            prevprojDtl = projectService.getProject(prevprojDtl.getProjectId(), prevprojDtl.getVersion());
            retErrors.put("version", prevprojDtl.getVersion());
            retErrors.putAll(CompareUtility.fullCompare(projectDetails, prevprojDtl));
            //TODO: prevProj version should not be stored at class level. 
            //Add into retErrors and make sure to remove in caller 
        }
        return retErrors;
    }

    /**
     * @param projectDetails Details of the project
     * @return the list of required File types
     */
    private List<String> validateGoldenCheckRules(ProjectDetails projectDetails) {
        List<String> requiredFilesTypes = null;
        String[] splitService = projectDetails.getServiceType().split(",");

        for (int i = 0; i < splitService.length; i++) {
            splitService[i] = splitService[i].substring(0, 1).toUpperCase() + splitService[i].substring(1, splitService[i].length()).toLowerCase();
            //Required files as per business rules            
            if (MappingUtil.getServiceId(splitService[i]) != null) {
                requiredFilesTypes = baseService.getServiceFiles(
                        MappingUtil.getServiceId(splitService[i])
                );
            }
        }
        //Types provided by customer
        List<String> availFileTypes = projectDetails.getFileDetails().stream()
                .filter(fd -> fd.getType() != null)
                .map(FileDetails::getType)
                .collect(Collectors.toList());
        //Formats
        Set<String> availFormats = projectDetails.getFileDetails().stream()
                .filter(fd -> fd.getFormat() != null)
                .map(FileDetails::getFormat)
                .collect(Collectors.toSet());
        
        availFileTypes.addAll( availFormats );
        //Find missing files types
        return CompareUtility.findMissingItems(requiredFilesTypes, availFileTypes); 
    }

    /**
     * This method save the details of the projectDetails into the database
     *
     * @param projectDetails Details of the project
     */
    public void save(ProjectDetails projectDetails) {
    	
    	if( StringUtils.isEmpty(projectDetails.getServiceType()) ) {
    		projectDetails.getErrors().put("V0000", "Service Type is required");
    		return;
    	} else {
	    	String[] splitServiceTypes = projectDetails.getServiceType().split(",");
	        for (int i = 0; i < splitServiceTypes.length; i++) {
	            String splitServiceType = splitServiceTypes[i].substring(0, 1).toUpperCase() + splitServiceTypes[i].substring(1, splitServiceTypes[i].length()).toLowerCase();
	            if (MappingUtil.getServiceId(splitServiceType) == null) {
	                projectDetails.getErrors().put("V0000", "Invalid Service Type - " + splitServiceTypes[i]);
	                return ;
	            }
	        }
    	}
        //If projectID/R# is not there, get it from FEMS API call. Stub the call for now
        //Check if new version is required or its an add/replace for existing version.
        String projectId = getProjectId(projectDetails);
        String version = getVersion(projectDetails);

        projectDetails.setProjectId(projectId);
        projectDetails.setVersion(version);

        processGerber(projectDetails.getFileDetails());

        //Save projectFiles
        projectDetails.getFileDetails().stream().forEach(fd -> {
            ProjectFiles pFiles = ReportUtility.convertToDBObject(fd);
            pFiles.setVersion(UUID.fromString(version));
            pFiles.setProjectId(projectId);
            projectFilesService.save(pFiles);
        });

        //Save into project table               
        projectService.save(ReportUtility.convertToDBObject(projectDetails));
    }

    /**
     * This method retrieves the projectId of that record from the database
     * while matching the record with customerId or emailAddress or zipFileName
     *
     * @param projectDetails Details of the project
     * @return the projectID of matching record
     */
    private String getProjectId(ProjectDetails projectDetails) {
    	
    	Map<String, String> projKeyMap = new HashMap<String, String>();
        //If exists in parameter object, return that
        if (!StringUtils.isEmpty(projectDetails.getProjectId())) {
            return projectDetails.getProjectId();
        }
        //For existing project ( customer forgot to pass projectID, we need to find it)
        if (!projectDetails.isNewProject()) {
            //Get by customerID - Best chance to find match with this
        	projKeyMap = getProjectIdByCustomerId(projectDetails.getCustomerId());
            if (StringUtils.isEmpty(projKeyMap.get("project_id"))) {
                //Get by customerEmal - next best chance to find match with this
            	projKeyMap = getProjectIdByCustomerEmail(projectDetails.getEmailAddress());
            }
            if (StringUtils.isEmpty(projKeyMap.get("project_id"))) {
                //Get by zipFileName - possible chance to find match with this
            	projKeyMap = getProjectIdByZipName(projectDetails.getZipFileName());
            }
        }
        //Still empty or a new project, CALL FEMS API to get it
        if (StringUtils.isEmpty(projKeyMap.get("project_id"))) {
            //TODO: when FEMS ready, we need to call API
            //projectId = Long.toHexString(Double.doubleToLongBits(Math.random()));
            projKeyMap.put("project_id", Long.toHexString(Double.doubleToLongBits(Math.random())));
        }
        if( projectDetails.isAttachReplace() ) {
        	projectDetails.setVersion(projKeyMap.get("version"));
        }
        return projKeyMap.get("project_id");
    }

    /**
     * This method retrieves the whole record of that projectId from the
     * database
     *
     * @param projectId the projectId of the record
     * @return the projectDetails of matching projectId
     */
    private ProjectDetails getLatestRecord(String projectId) {
        return getLatestRecord(projectService.findByKeyProjectId(projectId));
    }

    /**
     * Get the latest record from the list. Based on created date of the record.
     *
     * @param projDtl Details of the project
     * @return Null if not found. Else return the match
     */
    private ProjectDetails getLatestRecord(List<ProjectDetails> projDtl) {
        ProjectDetails latestRecord = null;
        if (projDtl != null && projDtl.size() > 0) {
            latestRecord = projDtl.stream()
                    .max((a1, a2) -> a1.getCreateDate().compareTo(a2.getCreateDate())).orElse(null);
        }
        return latestRecord;
    }

    /**
     * Get the previous record for the given project. Based on created date
     *
     * @param projDtl Details of the project
     * @return Null if not found. Else return the match
     */
    private ProjectDetails getPreviousRecord(ProjectDetails projDtl) {
        ProjectDetails prevRecord = null;

        List<ProjectDetails> allRecords = projectService.findByKeyProjectId(projDtl.getProjectId());
        if (allRecords != null) {
            // first remove the current record by removing same version record.
            // From the remaining, we will find the latest by created date
            prevRecord = allRecords.stream()
                    .filter(p -> !p.getVersion().equals(projDtl.getVersion()))
                    .max((a1, a2) -> a1.getCreateDate().compareTo(a2.getCreateDate())).orElse(null);

        }

        return prevRecord;
    }

    /**
     * Find the project by customerID
     *
     * @param customerId the customerId of the customer
     * @return projectID of matching record
     */
    private Map<String, String> getProjectIdByCustomerId(String customerId) {
    	Map<String, String> retMap = new HashMap<String, String>();
        if ( !StringUtils.isEmpty(customerId)) {
            
            List<ProjectDetails> projDtl = projectService.findByCustomerId(customerId);
            ProjectDetails latestRecord = getLatestRecord(projDtl);
            if (latestRecord != null) {
            	retMap.put("project_id", latestRecord.getProjectId());
            	retMap.put("version", latestRecord.getVersion());
            }
        }
        return retMap;
    }

    /**
     * Find the project by customerEmail
     *
     * @param emailId the email of the customer
     * @return projectID of matching record
     */
    private Map<String, String> getProjectIdByCustomerEmail(String emailId) {
    	Map<String, String> retMap = new HashMap<String, String>();
        if ( !StringUtils.isEmpty(emailId)) {
            
            List<ProjectDetails> projDtl = projectService.findByCustomerEmail(emailId);
            ProjectDetails latestRecord = getLatestRecord(projDtl);
            if (latestRecord != null) {
            	retMap.put("project_id", latestRecord.getProjectId());
            	retMap.put("version", latestRecord.getVersion());
            }
        }
        return retMap;
    }

    /**
     * Find the project by zipFileName
     *
     * @param zipFileName
     * @return projectID of matching record
     */
    private Map<String, String> getProjectIdByZipName(String zipFileName) {
    	Map<String, String> retMap = new HashMap<String, String>();
        if ( !StringUtils.isEmpty(zipFileName)) {
            
            List<ProjectDetails> projDtl = projectService.findByZipFileName(zipFileName);
            ProjectDetails latestRecord = getLatestRecord(projDtl);
            if (latestRecord != null) {
            	retMap.put("project_id", latestRecord.getProjectId());
            	retMap.put("version", latestRecord.getVersion());
            }
        }
        return retMap;
    }

    /**
     * Find the version for the given project.
     *
     * @param projectDetails
     * @return Create new if doesn't exist in given project
     */
    private String getVersion(ProjectDetails projectDetails) {
        String version = null;
        if (projectDetails.isAttachReplace()) {
            return projectDetails.getVersion();
        } else {
            version = UUIDs.timeBased().toString();
        }
        return version;
    }

    public void validateFiles(String projectId) {
        //TODO: implememt
        //Get the projectDetails by projectId
        //call validateFiles( ProjectDetails projectDetails ) to get results
    }

    /**
     * Extract and save the zip file. No validations.
     *
     * @param file
     * @param inputs
     * @return
     * @throws IOException
     */
    /**
     * public Set<String> extractAndSaveFiles(MultipartFile file,
     * CustomerInformation inputs) throws IOException { // Local file based
     * String fileName = util.storeFile(inputs.getProjectId(), file);
     * util.extractFiles(inputs.getProjectId(), fileName); //Path folder =
     * Paths.get(util.getUploadDir() + File.separator + inputs.getProjectId() +
     * File.separator).toAbsolutePath().normalize(); // END local	* //S3 Based
     * //util.storeFile(inputs.getProjectId(), file);
     * //report.setExctractedFileNames( util.listObjects(inputs.getProjectId())
     * ); // end S3 return util.listFiles(inputs.getProjectId()); }*
     */
    /**
     * Upload, extract and validates files
     *
     * @param file - the file to be uploaded
     * @param inputs - the inputs of CustomerInputs
     * @return Report - report with validation status
     */
    /**
     * public Report uploadAndExtractFile(MultipartFile file,
     * CustomerInformation inputs, PCBInformation boardInfo) throws Exception {
     *
     * ProjectDetails projectDetails = new ProjectDetails();
     * projectDetails.setProjectId(inputs.getProjectId());
     *
     * Report report = validateFiles(projectDetails);
     *
     * logger.debug("****** Done generating report *******");
     *
     * //To delete the folder Path folder = Paths.get(util.getUploadDir() +
     * File.separator +
     * projectDetails.getProjectId()).toAbsolutePath().normalize();
     * FileUtil.deleteFolder(folder.toFile()); return report; }*
     */
    /**
     * Performs all possible Gerber file processing.
     *
     * @param fileDetails - These given file details will be updated if we find
     * more details during processing
     */
    private void processGerber(List<FileDetails> fileDetails) {

        GerberFileProcessingUtil.processFilesByExtension(fileDetails, baseService.getExtensionToFileMapping());

        //For each file that is gerber format
        fileDetails.stream()
                .filter(fd -> StringUtils.isEmpty(fd.getType()))
                .forEach(fd -> {
                    //Apply rules by name pattern
                    GerberFileProcessingUtil.parseFileName(fd);
                });
    }

    /**
     * ODB processing. Mainly parse matrix file to get fileDetils
     *
     * @param folder
     * @param projectId
     * @return
     */
    private List<FileDetails> processODB(Path folder, String projectId) {
        //To check that whether file type is ODB or not.
        File[] listOfFiles = folder.toFile().listFiles();
        List<FileDetails> fdList = new ArrayList<FileDetails>();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isDirectory()) {
                if (listOfFiles[i].getName().toLowerCase().equals("odb")) {
                    Path checkODBFolder = Paths.get(util.getUploadDir() + File.separator + projectId + File.separator + listOfFiles[i].getName() + File.separator + "matrix" + File.separator + "matrix").toAbsolutePath().normalize();
                    if (checkODBFolder.toFile().exists()) {
                        fdList = ODBProcessing.processODB(checkODBFolder);
                        //Print Result 
                        //fdList.stream().forEach(fd->
                        //System.out.println(fd.getName()));
                    }
                }
            }
        }
        return fdList;
    }

}
