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

/**
 *
 * @author mccaffrey
 */
public class ForecasterUtils {

    public static Parameters run(Parameters inputParameters) {

        AdministrativeGender gender = ForecasterUtils.parsePatientGender(inputParameters);
        Date dob = ForecasterUtils.parsePatientDateOfBirth(inputParameters);
        Date assessmentDate = ForecasterUtils.parseAssessmentDate(inputParameters);
        List<Immunization> imms = ForecasterUtils.parseImmunizations(inputParameters);
        String serviceType = ForecasterUtils.parseServiceType(inputParameters);
        String serviceURL = ForecasterUtils.parseServiceURL(inputParameters);

        AdapterImpl adapter = null;
        
        // No longer needed
        /*
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
        } */
        adapter = new TCHAdapterImpl();            
        
        adapter.setAssessmentDate(assessmentDate);
        adapter.setDateOfBirth(dob);
        adapter.setGender(gender);
        adapter.setImmunizations(imms);
        adapter.setServiceType(serviceType);
        adapter.setServiceURL(serviceURL);

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

    //todo: make this more efficient with next method?
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

    public static final void main(String[] args) {
        /*
        Parameters inputParameters = new Parameters();
        ParametersParameterComponent service = new ParametersParameterComponent();
        service.set
        inputParameters.a
        Parameters returned = ForecasterUtils.run(inputParameters); 
        */
    }
    
}
