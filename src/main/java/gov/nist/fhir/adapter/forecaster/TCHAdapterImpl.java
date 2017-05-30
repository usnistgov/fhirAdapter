/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter.forecaster;

import ca.uhn.fhir.context.FhirContext;
import gov.nist.fhir.FHIRUtils;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Immunization.ImmunizationVaccinationProtocolComponent;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PositiveIntType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.codesystems.ImmunizationRecommendationDateCriterion;

import org.tch.fc.ConnectFactory;
import org.tch.fc.ConnectorInterface;
import org.tch.fc.model.EvaluationActual;
import org.tch.fc.model.Event;
import org.tch.fc.model.EventType;
import org.tch.fc.model.ForecastActual;
import org.tch.fc.model.Service;
import org.tch.fc.model.Software;
import org.tch.fc.model.SoftwareResult;
import org.tch.fc.model.TestCase;
import org.tch.fc.model.TestEvent;
import org.tch.fc.model.VaccineGroup;

/**
 *
 * @author mccaffrey
 */
public class TCHAdapterImpl implements AdapterImpl {

    private AdministrativeGender gender = null;
    private Date dateOfBirth = null;
    private Date assessmentDate = null;
    private List<Immunization> immunizations = null;
    private String serviceType = null;
    private String serviceURL = null;

    @Override
    public Parameters run() {

        Software software = TCHAdapterImpl.createSoftware(this.getServiceType(), this.getServiceURL());
        TestCase testCase = createTestCase(this.getGender(), this.getDateOfBirth(), this.getAssessmentDate(), this.getImmunizations());
        // List<TestEvent> events = testCase.getTestEventList();

        Parameters parameters = new Parameters();
        ParametersParameterComponent ppc = new ParametersParameterComponent();
        //TODO Put in Consts!
        ppc.setName("ImmunizationRecommendation");
        ImmunizationRecommendation ir = new ImmunizationRecommendation();
        //ir.setId(UUID.randomUUID().toString());

        parameters.getParameter().add(ppc);
        List<ForecastActual> forecastActualList = getForecasts(software, testCase);
        if (testCase.getTestEventList() != null) {

            for (int i = 0; i < testCase.getTestEventList().size(); i++) {

                TestEvent testEvent = testCase.getTestEventList().get(i);
                if (testEvent.getEvaluationActualList() != null) {
                    List<EvaluationActual> actuals = testEvent.getEvaluationActualList();
                    CodeableConcept cconcept = new CodeableConcept();
                    Coding coding = new Coding();
                    coding.setCode(testEvent.getEvent().getVaccineCvx());
                    cconcept.getCoding().add(coding);
                    Immunization immunization = new Immunization();
                   // immunization.setId(UUID.randomUUID().toString());
                    immunization.setVaccineCode(cconcept);
                    immunization.setDate(actuals.get(0).getTestEvent().getEventDate());
                    immunization.setPatient(new Reference().setReference("42"));
                    immunization.setNotGiven(false);
                    immunization.setPrimarySource(false);
                    immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
                    for (int j = 0; j < actuals.size(); j++) {
                        EvaluationActual actual = actuals.get(j);
                        ImmunizationVaccinationProtocolComponent ivp = new ImmunizationVaccinationProtocolComponent();
                        CodeableConcept doseValidConcept = ForecasterUtils.createCodeableConcept(actual.getDoseValid(), actual.getDoseValid(), null);
                        CodeableConcept doseStatus = new CodeableConcept();
                        if("Y".equalsIgnoreCase(doseValidConcept.getCodingFirstRep().getCode())) {
                            doseStatus.setText("Valid");
                            Coding code = new Coding();
                            code.setCode("Valid");
                            doseStatus.addCoding(code);
                        } else {
                            doseStatus.setText("Not Valid");
                            Coding code = new Coding();
                            code.setCode("not valid");
                            doseStatus.addCoding(code);
                        }
                        ivp.setDoseStatus(doseStatus);
                        //ivp.getTargetDisease().add(new CodeableConcept().setText("unknown"));
                        Coding cvxCoding = new Coding();
                        cvxCoding.setCode(actual.getSeriesUsedCode());
                        ivp.getTargetDisease().add(new CodeableConcept().addCoding(cvxCoding));
                        ivp.setSeries(actual.getSeriesUsedCode());
                        ivp.setDescription(actual.getSeriesUsedText());
                        ivp.setDoseStatusReason(ForecasterUtils.createCodeableConcept(actual.getReasonCode(), actual.getReasonText(), null));
                        ivp.addTargetDisease(new CodeableConcept());
                        // The spec says the Dose Sequence shall be a positive integer,
                        // however, the forecaster sometimes returns a char.  If it is
                        // good we pass it along.  If not, we drop it.                            
                        try {
                            PositiveIntType pi = new PositiveIntType();
                            pi.setValue(Integer.valueOf(actual.getDoseNumber()));
                            ivp.setDoseSequenceElement(pi);
                        } catch (Exception e) {
                            //TODO: Throw a warning for bad character?
                        }
                        immunization.getVaccinationProtocol().add(ivp);

                    }

                    Reference supportingImm = new Reference();
                    supportingImm.setResource(immunization);
                    
                    ImmunizationRecommendationRecommendationComponent irrc = new ImmunizationRecommendationRecommendationComponent();
                    irrc.addSupportingImmunization(supportingImm);

                    ir.addRecommendation(irrc);
                    /*
                    ir.getContained().add(new Patient().setGender(AdministrativeGender.FEMALE));

                    ir.getContained().add(immunization);

                    ir.getRecommendation().add(new ImmunizationRecommendationRecommendationComponent().setDoseNumber(123));
*/
                    //  ir.addContained(immunization);
                    System.out.println(ir.getContained().size());
                    //Resource resource = new Resource();
/*
                    //ContainedComponent con = new ContainedComponent();
                    FhirContext ctx = FhirContext.forDstu3();
                    System.out.println("Current after adding 1 contained = \n" + ctx.newXmlParser().setPrettyPrint(false).encodeResourceToString(ir));
                    System.out.println("JSON = " + ctx.newJsonParser().encodeResourceToString(ir));
                    System.out.println("imm = " + ctx.newXmlParser().encodeResourceToString(immunization));
                     */
                }

            }

            for (ForecastActual forecastActual : forecastActualList) {
                //ImmunizationRecommendationRecommendationComponent recommendation = TCHAdapterImpl.createForecastImmunizationRecommendation(forecastActual, this.getGender(), this.getDateOfBirth(), testCase.getTestEventList());
                ImmunizationRecommendationRecommendationComponent recommendation = TCHAdapterImpl.createImmunizationRecommendationRecommendationComponent(forecastActual);

                ir.getRecommendation().add(recommendation);
                // TODO do we need this?
                //parameters.setFullUrl(recommendation.getImplicitRules());
                // parameters.setId(recommendation.getIdentifier().get(0).getValue());

            }
            ppc.setResource(ir);
        }
        return parameters;
    }
/*
    public static ImmunizationRecommendationRecommendationComponent createForecastImmunizationRecommendationRecommendationComponent(ForecastActual i, List<TestEvent> events) {
        ImmunizationRecommendationRecommendationComponent o = new ImmunizationRecommendationRecommendationComponent();
        //o.setId(UUID.randomUUID().toString());
        Identifier identifier = new Identifier();
        identifier.setValue(UUID.randomUUID().toString());

        // TODO: Do we need to populate meta and identifier?
        //o.getIdentifier().add(identifier);
        //o.setMeta(createMeta(URIs.FORECAST_IMMUNIZATIONRECOMMENDATION));
        // TODO: No patient info needed here?  Check
        /*
        Patient patient = new Patient();
        patient.setGender(gender);
        patient.setBirthDate(dob);
        o.getContained().add(patient);
         
        //ImmunizationRecommendationRecommendationComponent irr = createImmunizationRecommendationRecommendationComponent(i);
        //o.getRecommendation().add(irr);
        return o;
    }
*/
    public static ImmunizationRecommendationRecommendationComponent createImmunizationRecommendationRecommendationComponent(
            ForecastActual i) {
        ImmunizationRecommendationRecommendationComponent o = new ImmunizationRecommendationRecommendationComponent();

        //o.setId(UUID.randomUUID().toString());
        o.setDate(i.getDueDate());
        CodeableConcept code = new CodeableConcept();
        code.setText(i.getVaccineGroup().getLabel());

        Coding coding = new Coding();

        coding.setCode(i.getVaccineGroup().getVaccineCvx());
        code.getCoding().add(coding);
        o.setVaccineCode(code);
        
        if (i.getDoseNumber() != null && !i.getDoseNumber().isEmpty()) {
            try {
                o.setDoseNumber(Integer.valueOf(i.getDoseNumber()));
            } catch (Exception e) {
                System.out.println("Bad dose number");
                // TODO: Bad Dose value (is supposed to be a positive int!)
            }
        } else {
            System.out.println("No dose number");
        }
        
        if (i.getAdminStatus() != null) {
            System.out.println("Admin Status = " + i.getAdminStatus());
            CodeableConcept adminStatus = new CodeableConcept();
            Coding adminStatusCoding = new Coding();
            if (i.getAdminStatus().equalsIgnoreCase("A")) {
                adminStatusCoding.setCode("assumed complete or immune");
            } else if (i.getAdminStatus().equalsIgnoreCase("C")) {
                adminStatusCoding.setCode("complete");                
            } else if (i.getAdminStatus().equalsIgnoreCase("D")) {
                adminStatusCoding.setCode("due");
            } else if(i.getAdminStatus().equalsIgnoreCase("E")) {
                adminStatusCoding.setCode("error");
            } else if(i.getAdminStatus().equalsIgnoreCase("F")) {
                adminStatusCoding.setCode("finished");
            } else if(i.getAdminStatus().equalsIgnoreCase("G")) {
                adminStatusCoding.setCode("aged out");
            } else if(i.getAdminStatus().equalsIgnoreCase("I")) {
                adminStatusCoding.setCode("immune");                
            } else if(i.getAdminStatus().equalsIgnoreCase("L")) {
                adminStatusCoding.setCode("due later");
            } else if(i.getAdminStatus().equalsIgnoreCase("N")) {
                adminStatusCoding.setCode("not complete");
            } else if(i.getAdminStatus().equalsIgnoreCase("O")) {
                adminStatusCoding.setCode("overdue");
            } else if(i.getAdminStatus().equalsIgnoreCase("R")) {
                adminStatusCoding.setCode("no results");
            } else if(i.getAdminStatus().equalsIgnoreCase("S")) {
                adminStatusCoding.setCode("complete for season");                
            } else if(i.getAdminStatus().equalsIgnoreCase("U")) {
                adminStatusCoding.setCode("unknown");                
            } else if(i.getAdminStatus().equalsIgnoreCase("V")) {
                adminStatusCoding.setCode("Consider");
            } else if(i.getAdminStatus().equalsIgnoreCase("W")) {
                adminStatusCoding.setCode("waivered");
            } else if(i.getAdminStatus().equalsIgnoreCase("X")) {
                adminStatusCoding.setCode("contraindicated");
            } else if(i.getAdminStatus().equalsIgnoreCase("Z")) {
                adminStatusCoding.setCode("recommended but not required");
            } else {
                System.out.println("Admin status not recognized = " + i.getAdminStatus());
            }            
            adminStatus.getCoding().add(adminStatusCoding);
            o.setForecastStatus(adminStatus);
        } else {            
            System.out.println("No admin status");
            o.setForecastStatus(new CodeableConcept().setText("unknown"));
        }
        CodeableConcept forecastStatus = new CodeableConcept();
        forecastStatus.getCoding().add(FHIRUtils.IMMUNIZATION_RECOMMENDATION_STATUS.DUE.coding);
        System.out.println("Due = " + i.getDueDate());        
        System.out.println("Finished = " + i.getFinishedDate());
        System.out.println("OverDue = " + i.getOverdueDate());
        System.out.println("Valid = " + i.getValidDate());
        
        //TODO: Clean up by moving some strings to Consts
        if (i.getDueDate() != null && !"".equals(i.getDueDate())) {
            // It is "due" in TCH, but "recommended" in FHIR
            ImmunizationRecommendationRecommendationDateCriterionComponent dueCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            dueCriterion.setValue(i.getDueDate());
            CodeableConcept due = new CodeableConcept();
            Coding dueCode = new Coding();
            dueCode.setCode("recommended");
            dueCode.setSystem("http://hl7.org/fhir/immunization-recommendation-date-criterion");
            dueCode.setDisplay("Recommended");
            due.getCoding().add(dueCode);
            dueCriterion.setCode(due);
            o.getDateCriterion().add(dueCriterion);
        }
        if (i.getValidDate() != null && !"".equals(i.getValidDate())) {
            ImmunizationRecommendationRecommendationDateCriterionComponent earliestCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            earliestCriterion.setValue(i.getValidDate());
            CodeableConcept earliest = new CodeableConcept();
            Coding earliestCode = new Coding();
            earliestCode.setCode("earliest");
            earliestCode.setSystem("http://hl7.org/fhir/immunization-recommendation-date-criterion");
            earliestCode.setDisplay("Earliest Date");
            earliest.getCoding().add(earliestCode);
            earliestCriterion.setCode(earliest);
            o.getDateCriterion().add(earliestCriterion);
        }

        if (i.getOverdueDate() != null && !"".equals(i.getOverdueDate())) {
            ImmunizationRecommendationRecommendationDateCriterionComponent overdueCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            overdueCriterion.setValue(i.getOverdueDate());
            CodeableConcept overdue = new CodeableConcept();
            Coding overdueCode = new Coding();
            overdueCode.setCode("overdue");
            overdueCode.setSystem("http://hl7.org/fhir/immunization-recommendation-date-criterion");
            overdueCode.setDisplay("Past Due Date");
            overdue.getCoding().add(overdueCode);
            overdueCriterion.setCode(overdue);
            o.getDateCriterion().add(overdueCriterion);
        }
        
        if (i.getFinishedDate() != null && !"".equals(i.getFinishedDate())) {
            ImmunizationRecommendationRecommendationDateCriterionComponent latestCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            latestCriterion.setValue(i.getFinishedDate());
            CodeableConcept latest = new CodeableConcept();
            Coding latestCode = new Coding();
            latestCode.setCode("latest");
            latestCode.setSystem("http://hl7.org/fhir/immunization-recommendation-date-criterion");
            latestCode.setDisplay("Latest");
            latest.getCoding().add(latestCode);
            latestCriterion.setCode(latest);
            o.getDateCriterion().add(latestCriterion);
        }
        /*
            
            ImmunizationRecommendationDateCriterion dueCriterion = createImmunizationRecommendationDateCriterionComponent(
                    FHIRUtils.IMMUNIZATION_RECOMMENDATION_DATE_CRITERION.DUE, i.getDueDate());
            o.getD
            o.getDateCriterion().add(dueCriterion);
            ImmunizationRecommendationDateCriterion earliestCriterion = createImmunizationRecommendationDateCriterion(
                    FHIRUtils.IMMUNIZATION_RECOMMENDATION_DATE_CRITERION.EARLIEST, i.getDueDate());
            o.getDateCriterion().add(earliestCriterion);
            ImmunizationRecommendationDateCriterion overdueCriterion = createImmunizationRecommendationDateCriterion(
                    FHIRUtils.IMMUNIZATION_RECOMMENDATION_DATE_CRITERION.OVERDUE, i.getOverdueDate());
            o.getDateCriterion().add(overdueCriterion);
            ImmunizationRecommendationDateCriterion latestCriterion = createImmunizationRecommendationDateCriterion(
                    FHIRUtils.IMMUNIZATION_RECOMMENDATION_DATE_CRITERION.LATEST, i.getOverdueDate());
            o.getDateCriterion().add(latestCriterion);
         */
        return o;
    }

    /*
    public static ImmunizationRecommendationDateCriterion createImmunizationRecommendationDateCriterionComponent(
            FHIRUtils.IMMUNIZATION_RECOMMENDATION_DATE_CRITERION crit, java.util.Date date) {
        ImmunizationRecommendationRecommendationDateCriterionComponent dateCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
        dateCriterion.
        CodeableConcept forecastStatus = FhirFactory.eINSTANCE.createCodeableConcept();
        Coding coding = FhirFactory.eINSTANCE.createCoding();
        //coding.setCode(crit.coding.getCode());
        Code code = FhirFactory.eINSTANCE.createCode();
        code.setValue(crit.coding.getCode().getValue());
        coding.setCode(code);
        //forecastStatus.getCoding().add(crit.coding);                
        forecastStatus.getCoding().add(coding);
//               forecastStatus.getCoding().add(crit);
        //  forecastStatus.setText(FHIRUtil.convert(crit.coding.getCode().getValue()));
        //   forecastStatus.setId(crit.name() + " " + crit.toString());
        dateCriterion.setCode(forecastStatus);
        dateCriterion.setValue(FHIRUtil.convertDateTime(date));

        return dateCriterion;
    }
     */
    public static Software createSoftware(String type, String url) {
        Software software = new Software();
        software.setServiceUrl(url);
        Service service = Service.getService(type);
        software.setService(service);
        return software;
    }

    public static TestCase createTestCase(AdministrativeGender gender, Date dob, Date assessmentDate, List<Immunization> immunizations) {
        TestCase testCase = new TestCase();

        testCase.setEvalDate(assessmentDate);
        testCase.setPatientDob(dob);
        //TODO: double check this is correct
        testCase.setPatientSex(gender.toCode().substring(0, 1));

        List<TestEvent> events = createTestEvents(immunizations);
        testCase.setTestEventList(events);
        return testCase;

    }

    public static List<TestEvent> createTestEvents(List<Immunization> immunizations) {
        List<TestEvent> events = new ArrayList<TestEvent>();
        Iterator<Immunization> it = immunizations.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            Immunization imm = it.next();
            //TODO: Error checking
            String code = imm.getVaccineCode().getCoding().get(0).getCode();
            Date date = imm.getDate();

            TestEvent testEvent = new TestEvent(i, date);
            Event event = new Event();
            event.setEventType(EventType.VACCINATION);
            event.setVaccineCvx(code);
            testEvent.setEvent(event);
            events.add(testEvent);
        }
        return events;
    }

    static List<ForecastActual> getForecasts(Software software, TestCase testCase) {
        List<ForecastActual> forecastActualList = null;
        try {
            ConnectorInterface connector = ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
            forecastActualList = connector.queryForForecast(testCase, new SoftwareResult());
        } catch (Exception e) {
            e.printStackTrace();
            //todo: error handling
        }
        return forecastActualList;
    }

    /**
     * @return the gender
     */
    public AdministrativeGender getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public void setGender(AdministrativeGender gender) {
        this.gender = gender;
    }

    /**
     * @return the dateOfBirth
     */
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * @param dateOfBirth the dateOfBirth to set
     */
    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * @return the assessmentDate
     */
    public Date getAssessmentDate() {
        return assessmentDate;
    }

    /**
     * @param assessmentDate the assessmentDate to set
     */
    public void setAssessmentDate(Date assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    /**
     * @return the immunizations
     */
    public List<Immunization> getImmunizations() {
        return immunizations;
    }

    /**
     * @param immunizations the immunizations to set
     */
    public void setImmunizations(List<Immunization> immunizations) {
        this.immunizations = immunizations;
    }

    /**
     * @return the serviceType
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * @param serviceType the serviceType to set
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * @return the serviceURL
     */
    public String getServiceURL() {
        return serviceURL;
    }

    /**
     * @param serviceURL the serviceURL to set
     */
    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public static void main(String args[]) {
        /*
        ImmunizationRecommendation ir = new ImmunizationRecommendation();
        Immunization imm = new Immunization();
        imm.setId(UUID.randomUUID().toString());
        imm.setStatus(Immunization.ImmunizationStatus.NULL);
        imm.setNotGiven(true);
        imm.setVaccineCode(new CodeableConcept().addCoding(new Coding().setCode("123")));
        imm.setPatient(new Reference().setReference("12345"));
        imm.setPrimarySource(true);
    Identifier i = new Identifier();
        i.setUse(Identifier.IdentifierUse.USUAL);
        i.setSystem("urn:blahblah");
        i.setValue("ABC123");
        imm.getIdentifier().add(i);
        Reference ref = new Reference();
        ref.setResource(ir);
        
        ir.addIdentifier(imm.getIdentifierFirstRep());
        ir.setId("123");
        imm.setId("123");
        ImmunizationRecommendationRecommendationComponent irrc = new ImmunizationRecommendationRecommendationComponent();
        irrc.getSupportingImmunization().add(ref);
        ir.addRecommendation(irrc);

        System.out.println(ir.getContained().size());
        FhirContext ctx = FhirContext.forDstu3();        
        System.out.println(ctx.newXmlParser().setPrettyPrint(false).encodeResourceToString(ir));
         */

        // Create an Immunization Resource
        /*
 Immunization immunization = new Immunization();
 
 // Set the elements

 immunization.setPrimarySource(false);
 immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);


 
 // Create a Reference and add the resource
 Reference supportingImmReference = new Reference();
 supportingImmReference.setResource(immunization);
 
 ImmunizationRecommendationRecommendationComponent irrc = new ImmunizationRecommendationRecommendationComponent();
 
 // Add the reference to the ImmunizationRecommendation Resource
 irrc.addSupportingImmunization(supportingImmReference);
ImmunizationRecommendation ir = new ImmunizationRecommendation();
 ir.addRecommendation(irrc);
 
 // Print it out...
      FhirContext ctx = FhirContext.forDstu3(); 
 System.out.println(ctx.newXmlParser().encodeResourceToString(ir));
 
         */
        // Create an Immunization Resource
        Immunization immunization = new Immunization();

// Set the elements
        //immFHIR.setWasNotGiven(false);
        immunization.setPrimarySource(false);
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);

        immunization.setVaccineCode(new CodeableConcept().addCoding(new Coding().setCode("55")));

// Create a Reference and add the resource
        Reference supportingImmReference = new Reference();
        supportingImmReference.setResource(immunization);

// Add the reference to the ImmunizationRecommendation Resource\
        ImmunizationRecommendationRecommendationComponent fcastFHIR = new ImmunizationRecommendationRecommendationComponent();
        fcastFHIR.addSupportingImmunization(supportingImmReference);

        ImmunizationRecommendation ir = new ImmunizationRecommendation();
        ir.addRecommendation(fcastFHIR);
        
// Print it out...
        FhirContext ctx = FhirContext.forDstu3();
        System.out.print(ctx.newXmlParser().encodeResourceToString(ir));

    }

}
