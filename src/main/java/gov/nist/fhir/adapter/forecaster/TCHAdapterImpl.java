/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter.forecaster;

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
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PositiveIntType;
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
        List<TestEvent> events = testCase.getTestEventList();

        Parameters parameters = new Parameters();

        List<ForecastActual> forecastActualList = getForecasts(software, testCase);

        for (ForecastActual forecastActual : forecastActualList) {
            ImmunizationRecommendation recommendation = TCHAdapterImpl.createForecastImmunizationRecommendation(forecastActual, this.getGender(), this.getDateOfBirth(), testCase.getTestEventList());
            
            
            
            
            
            parameters.setId(recommendation.getIdentifier().get(0).getValue());
            
            // TODO do we need this?
            //parameters.setFullUrl(recommendation.getImplicitRules());



        }

        if (testCase.getTestEventList() != null) {
            for (int i = 0; i < testCase.getTestEventList().size(); i++) {

                TestEvent testEvent = testCase.getTestEventList().get(i);
                if (testEvent.getEvaluationActualList() != null) {
                    List<EvaluationActual> actuals = testEvent.getEvaluationActualList();

                    BundleEntry entry = FhirFactory.eINSTANCE.createBundleEntry();
                    ResourceContainer container = FhirFactory.eINSTANCE.createResourceContainer();
                    Immunization immunization = FhirFactory.eINSTANCE.createImmunization();
                    CodeableConcept cconcept = FhirFactory.eINSTANCE.createCodeableConcept();
                    Coding coding = FhirFactory.eINSTANCE.createCoding();
                    Code code = FhirFactory.eINSTANCE.createCode();
                    code.setValue(testEvent.getEvent().getVaccineCvx());
                    cconcept.getCoding().add(coding);
                    coding.setCode(code);
                    immunization.setVaccineCode(cconcept);

                    for (int j = 0; j < actuals.size(); j++) {
                        EvaluationActual actual = actuals.get(j);
                        ImmunizationVaccinationProtocol ivp = FhirFactory.eINSTANCE.createImmunizationVaccinationProtocol();
                        CodeableConcept doseValidConcept = ForecastUtil.createCodeableConcept(actual.getDoseValid(), actual.getDoseValid(), null);
                        ivp.setDoseStatus(doseValidConcept);
                        ivp.setSeries(FHIRUtil.convert(actual.getSeriesUsedCode()));
                        ivp.setDescription(FHIRUtil.convert(actual.getSeriesUsedText()));
                        ivp.setDoseStatusReason(ForecastUtil.createCodeableConcept(actual.getReasonCode(), actual.getReasonText(), null));

                        // The spec says the Dose Sequence shall be a positive integer,
                        // however, the forecaster sometimes returns a char.  If it is
                        // good we pass it along.  If not, we drop it.                            
                        try {
                            PositiveInt pi = FhirFactory.eINSTANCE.createPositiveInt();
                            pi.setValue(new BigInteger(actual.getDoseNumber()));
                            ivp.setDoseSequence(pi);
                        } catch (Exception e) {
                        }

                        immunization.getVaccinationProtocol().add(ivp);

                        /*Date date = actual.getTestEvent().getEventDate();
                                
                                DateTime date = FhirFactory.eINSTANCE.createDateTime();
                                XMLGregorianCalendar calenderValue = null;
                                date.setValue(calenderValue);
                                date = actual.getTestEvent().getEventDate();
                         */
                        immunization.setDate(FHIRUtil.convertDateTime(actual.getTestEvent().getEventDate()));

                    }
                    container.setImmunization(immunization);
                    entry.setResource(container);
                    bundle.getEntry().add(entry);
                }

            }
        }

    }

    public static ImmunizationRecommendation createForecastImmunizationRecommendation(ForecastActual i, AdministrativeGender gender, Date dob,
            List<TestEvent> events) {
        ImmunizationRecommendation o = new ImmunizationRecommendation();
        o.setId(UUID.randomUUID().toString());
        Identifier identifier = new Identifier();
        identifier.setValue(UUID.randomUUID().toString());
        o.getIdentifier().add(identifier);
        // TODO: Do we need to populate meta?
        //o.setMeta(createMeta(URIs.FORECAST_IMMUNIZATIONRECOMMENDATION));

        Patient patient = new Patient();
        patient.setGender(gender);
        patient.setBirthDate(dob);
        o.getContained().add(patient);

        ImmunizationRecommendationRecommendationComponent irr = createImmunizationRecommendationRecommendationComponent(i);
        o.getRecommendation().add(irr);
        return o;
    }

    public static ImmunizationRecommendationRecommendationComponent createImmunizationRecommendationRecommendationComponent(
            ForecastActual i) {
        ImmunizationRecommendationRecommendationComponent o = new ImmunizationRecommendationRecommendationComponent();

        o.setId(UUID.randomUUID().toString());
        o.setDate(i.getDueDate());
        CodeableConcept code = new CodeableConcept();
        code.setText(i.getVaccineGroup().getVaccineCvx());

        Coding coding = new Coding();

        coding.setCode(i.getVaccineGroup().getVaccineCvx());
        code.getCoding().add(coding);
        o.setVaccineCode(code);

        PositiveIntType pi = new PositiveIntType();

        if (i.getDoseNumber() != null && !"".isEmpty()) {
            try {
                pi.setValue(new Integer(i.getDoseNumber()));
                o.setDoseNumberElement(pi);

            } catch (Exception e) {
                // TODO: Bad Dose value (is supposed to be a positive int!)
            }

            CodeableConcept adminStatus = new CodeableConcept();
            Coding adminStatusCoding = new Coding();
            adminStatusCoding.setCode(i.getAdminStatus());
            adminStatus.getCoding().add(adminStatusCoding);
            o.setForecastStatus(adminStatus);

            CodeableConcept forecastStatus = new CodeableConcept();
            forecastStatus.getCoding().add(FHIRUtils.IMMUNIZATION_RECOMMENDATION_STATUS.DUE.coding);

            
            ImmunizationRecommendationRecommendationDateCriterionComponent dueCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            dueCriterion.setValue(i.getDueDate());
            CodeableConcept due = new CodeableConcept();
            Coding dueCode = new Coding();
            dueCode.setCode("due");            
            due.getCoding().add(dueCode);
            dueCriterion.setCode(due);
            o.getDateCriterion().add(dueCriterion);
            
            ImmunizationRecommendationRecommendationDateCriterionComponent earliestCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            earliestCriterion.setValue(i.getDueDate());
            CodeableConcept earliest = new CodeableConcept();
            Coding earliestCode = new Coding();
            earliestCode.setCode("earliest");            
            earliest.getCoding().add(earliestCode);
            earliestCriterion.setCode(earliest);
            o.getDateCriterion().add(earliestCriterion);
            
            ImmunizationRecommendationRecommendationDateCriterionComponent overdueCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            overdueCriterion.setValue(i.getOverdueDate());
            CodeableConcept overdue = new CodeableConcept();
            Coding overdueCode = new Coding();
            overdueCode.setCode("overdue");            
            overdue.getCoding().add(overdueCode);
            overdueCriterion.setCode(overdue);
            o.getDateCriterion().add(overdueCriterion);
            
            ImmunizationRecommendationRecommendationDateCriterionComponent latestCriterion = new ImmunizationRecommendationRecommendationDateCriterionComponent();
            latestCriterion.setValue(i.getOverdueDate());
            CodeableConcept latest = new CodeableConcept();
            Coding latestCode = new Coding();
            latestCode.setCode("latest");            
            latest.getCoding().add(latestCode);
            latestCriterion.setCode(latest);
            o.getDateCriterion().add(latestCriterion);
            
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
  
        }
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

}
