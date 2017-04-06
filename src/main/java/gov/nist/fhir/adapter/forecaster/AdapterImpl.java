/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter.forecaster;

import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;

/**
 *
 * @author mccaffrey
 */
public interface AdapterImpl {
    
    /*
    private AdministrativeGender gender = null;
    private Date dateOfBirth = null;
    private Date assessmentDate = null;
    private List<Immunization> immunizations = null;
    */

    public Parameters run();
    
    
    /**
     * @return the gender
     */
    public AdministrativeGender getGender();
    
    /**
     * @param gender the gender to set
     */
    public void setGender(AdministrativeGender gender);
    
    /**
     * @return the dateOfBirth
     */
    public Date getDateOfBirth();

    /**
     * @param dateOfBirth the dateOfBirth to set
     */
    public void setDateOfBirth(Date dateOfBirth);

    /**
     * @return the assessmentDate
     */
    public Date getAssessmentDate();

    /**
     * @param assessmentDate the assessmentDate to set
     */
    public void setAssessmentDate(Date assessmentDate);

    /**
     * @return the immunizations
     */
    public List<Immunization> getImmunizations();

    /**
     * @param immunizations the immunizations to set
     */
    public void setImmunizations(List<Immunization> immunizations);
    
    public String getServiceType();
    public void setServiceType(String serviceType);
    
    public String getServiceURL();
    public void setServiceURL(String serviceURL);
    
}
