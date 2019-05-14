/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter.forecaster;

import gov.nist.fhir.Consts;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.immregistries.vfa.connect.model.ForecastEngineIssue;
import org.immregistries.vfa.connect.model.ForecastEngineIssueLevel;
import org.immregistries.vfa.connect.model.ForecastEngineIssueType;

/**
 *
 * @author mccaffrey
 */
public class ForecasterUtils {

    public static final String ISSUEDIVIDER = "$$$$$$$";
    public static final String SUBISSUEDIVIDER = "&&&&&&&";

    public static Parameters run(Parameters inputParameters) {

        AdministrativeGender gender = ForecasterUtils.parsePatientGender(inputParameters);
        Date dob = ForecasterUtils.parsePatientDateOfBirth(inputParameters);
        Date assessmentDate = ForecasterUtils.parseAssessmentDate(inputParameters);
        List<Immunization> imms = ForecasterUtils.parseImmunizations(inputParameters);
        String serviceType = ForecasterUtils.parseServiceType(inputParameters);
        String serviceURL = ForecasterUtils.parseServiceURL(inputParameters);
        String userId = ForecasterUtils.parseUserId(inputParameters);
        String facilityId = ForecasterUtils.parseFacilityId(inputParameters);
        String password = ForecasterUtils.parsePassword(inputParameters);

        AdapterImpl adapter = null;
        /*
        
        No longer needed 9/11/2018
        
        if (serviceType.equalsIgnoreCase("TCH")) {
            adapter = new TCHAdapterImpl();
        } else if(serviceType.equalsIgnoreCase("ICE")) {
            adapter = new TCHAdapterImpl();
        } else if(serviceType.equalsIgnoreCase("STC")) {
            adapter = new TCHAdapterImpl();
        } else if(serviceType.equalsIgnoreCase("SWP")) {
            adapter = new TCHAdapterImpl();
        } else if(serviceType.equalsIgnoreCase("MA")) {
            adapter = new TCHAdapterImpl();            
        } else {
            return null;
        }
         */
        adapter = new TCHAdapterImpl();

        adapter.setAssessmentDate(assessmentDate);
        adapter.setDateOfBirth(dob);
        adapter.setGender(gender);
        adapter.setImmunizations(imms);
        adapter.setServiceType(serviceType);
        adapter.setServiceURL(serviceURL);
        adapter.setUserId(userId);
        adapter.setFacilityId(facilityId);
        adapter.setPassword(password);

        return adapter.run();

    }

    public static CodeableConcept createCodeableConcept(java.lang.String code, java.lang.String text,
            java.lang.String uri) {
        CodeableConcept cc = new CodeableConcept();
        Coding coding = new Coding();
        coding.setId(UUID.randomUUID().toString());
        coding.setSystem(uri);
        coding.setDisplay(text);
        coding.setCode(code);
        cc.getCoding().add(coding);
        return cc;
    }

    public static ParametersParameterComponent parseSingleParametersParameterComponent(Parameters inputParameters, String parameterName) {

        List<ParametersParameterComponent> parameters = inputParameters.getParameter();
        Iterator<ParametersParameterComponent> it = parameters.iterator();
        while (it.hasNext()) {
            ParametersParameterComponent parameter = it.next();
            if (parameter.getName().equals(parameterName)) {
                return parameter;
            }
        }
        return null;
    }

    public static List<ParametersParameterComponent> parseMultipleParametersParameterComponent(Parameters inputParameters, String parameterName) {

        List<ParametersParameterComponent> parameters = inputParameters.getParameter();
        Iterator<ParametersParameterComponent> it = parameters.iterator();
        List<ParametersParameterComponent> found = new ArrayList<ParametersParameterComponent>();
        while (it.hasNext()) {
            ParametersParameterComponent parameter = it.next();
            if (parameter.getName().equals(parameterName)) {
                found.add(parameter);
            }
        }
        return found;

    }

    public static AdministrativeGender parsePatientGender(Parameters inputParameters) {
        Patient patient = ForecasterUtils.parsePatient(inputParameters);
        if (patient == null) {
            return null;
        }
        return patient.getGender();
    }

    public static Date parsePatientDateOfBirth(Parameters inputParameters) {
        Patient patient = ForecasterUtils.parsePatient(inputParameters);
        if (patient == null) {
            return null;
        }
        return patient.getBirthDate();
    }

    public static Date parseAssessmentDate(Parameters inputParameters) {
        ParametersParameterComponent assessmentPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_ASSESSMENT_DATE);
        if (assessmentPC == null) {
            return null;
        }
        Type value = assessmentPC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof DateType)) {
            return null;
        }
        DateType date = (DateType) value;
        return date.getValue();
    }

    public static Patient parsePatient(Parameters inputParameters) {
        ParametersParameterComponent patientPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_PATIENT);
        if (patientPC == null) {
            return null;
        }
        Resource patientR = patientPC.getResource();
        if (!(patientR instanceof Patient)) {
            return null;
        }
        return (Patient) patientR;
    }

    public static List<Immunization> parseImmunizations(Parameters inputParameters) {
        List<ParametersParameterComponent> parameters = ForecasterUtils.parseMultipleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_IMMUNIZATION);
        List<Immunization> immunizations = new ArrayList<Immunization>();
        Iterator<ParametersParameterComponent> it = parameters.iterator();
        while (it.hasNext()) {
            ParametersParameterComponent parameter = it.next();
            Resource resource = parameter.getResource();
            if (resource instanceof Immunization) {
                immunizations.add((Immunization) resource);
            }
        }
        return immunizations;
    }

    //todo: make this more efficient with next method? This was fine when there was just one thing to parse... not when there are several
    public static String parseServiceType(Parameters inputParameters) {
        ParametersParameterComponent serviceTypePC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_SERVICE_TYPE);
        if (serviceTypePC == null) {
            return null;
        }
        Type value = serviceTypePC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringType)) {
            return null;
        }
        StringType stringType = (StringType) value;
        return stringType.getValue();
    }

    public static String parseServiceURL(Parameters inputParameters) {
        ParametersParameterComponent serviceURLPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_SERVICE_URL);
        if (serviceURLPC == null) {
            return null;
        }
        Type value = serviceURLPC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringType)) {
            return null;
        }
        StringType stringType = (StringType) value;
        return stringType.getValue();
    }

    public static String parseUserId(Parameters inputParameters) {
        ParametersParameterComponent userIdPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_USER_ID);
        if (userIdPC == null) {
            return null;
        }
        Type value = userIdPC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringType)) {
            return null;
        }
        StringType stringType = (StringType) value;
        return stringType.getValue();
    }

    public static String parseFacilityId(Parameters inputParameters) {
        ParametersParameterComponent facilityIdPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_FACILITY_ID);
        if (facilityIdPC == null) {
            return null;
        }
        Type value = facilityIdPC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringType)) {
            return null;
        }
        StringType stringType = (StringType) value;
        return stringType.getValue();
    }

    public static String parsePassword(Parameters inputParameters) {
        ParametersParameterComponent passwordPC = ForecasterUtils.parseSingleParametersParameterComponent(inputParameters, Consts.PARAMETER_NAME_PASSWORD);
        if (passwordPC == null) {
            return null;
        }
        Type value = passwordPC.getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof StringType)) {
            return null;
        }
        StringType stringType = (StringType) value;
        return stringType.getValue();
    }

    public static String convertIssuesToString(List<ForecastEngineIssue> issues) {
        if (issues == null) {
            return "";
        }
        StringBuilder string = new StringBuilder();
        Iterator<ForecastEngineIssue> it = issues.iterator();
        while (it.hasNext()) {

            ForecastEngineIssue issue = it.next();
            string.append(issue.getIssueLevel().toString());
            string.append(ForecasterUtils.SUBISSUEDIVIDER);
            string.append(issue.getIssueType().toString());
            string.append(ForecasterUtils.SUBISSUEDIVIDER);
            string.append(issue.getDescription());
            string.append(ForecasterUtils.ISSUEDIVIDER);

        }
        return string.toString();
    }

    public static List<ForecastEngineIssue> convertStringToIssues(String string) {
        if (string == null) {
            return new ArrayList<>();
        }

        List<ForecastEngineIssue> feIssues = new ArrayList<>();

        String issues[] = string.split(ForecasterUtils.ISSUEDIVIDER);

        for (int i = 0; i < issues.length; i++) {
            ForecastEngineIssue issue = new ForecastEngineIssue();
            String issueString = issues[i];
            String subIssue[] = issueString.split(ForecasterUtils.SUBISSUEDIVIDER);
            issue.setIssueLevel(ForecastEngineIssueLevel.valueOf(subIssue[0]));
System.out.println("subissue = " + subIssue[1]);
            switch (subIssue[1]) {
/*
                case "MATCH_NOT_FOUND":
                    issue.setIssueType(ForecastEngineIssueType.MATCH_NOT_FOUND);
                    break;
                case "AUTHENTICATION_FAILURE":
                    issue.setIssueType(ForecastEngineIssueType.AUTHENTICATION_FAILURE);
                    break;
                case "ENGINE_NOT_AVAILABLE":
                    issue.setIssueType(ForecastEngineIssueType.ENGINE_NOT_AVAILABLE);
                    break;
*/
                case "UNEXPECTED_FORMAT":
                    issue.setIssueType(ForecastEngineIssueType.UNEXPECTED_FORMAT);
                    break;
                default:

            }          
            issue.setDescription(subIssue[2]);
            feIssues.add(issue);
        }

        return feIssues;

    }

    public static final void main(String[] args) {
        /*
        Parameters inputParameters = new Parameters();
        ParametersParameterComponent service = new ParametersParameterComponent();
        service.set
        inputParameters.a
        Parameters returned = ForecasterUtils.run(inputParameters); 
         */

        List<ForecastEngineIssue> feIssues1 = new ArrayList<>();
/*
        ForecastEngineIssue issue1 = new ForecastEngineIssue();
        issue1.setIssueType(ForecastEngineIssueType.MATCH_NOT_FOUND);
        issue1.setIssueLevel(ForecastEngineIssueLevel.ERROR);
        issue1.setDescription("Bad thing 1");
        feIssues1.add(issue1);

        ForecastEngineIssue issue2 = new ForecastEngineIssue();
        issue2.setIssueType(ForecastEngineIssueType.ENGINE_NOT_AVAILABLE);
        issue2.setIssueLevel(ForecastEngineIssueLevel.WARNING);
        issue2.setDescription("Bad thing 2");
        feIssues1.add(issue2);
*/
        String outputString1 = ForecasterUtils.convertIssuesToString(feIssues1);
        System.out.println(outputString1);

        List<ForecastEngineIssue> feIssues2 = ForecasterUtils.convertStringToIssues(outputString1);
        String outputString2 = ForecasterUtils.convertIssuesToString(feIssues2);
        System.out.println(outputString2);

    }

}
