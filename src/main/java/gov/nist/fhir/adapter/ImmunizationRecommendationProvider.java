/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import gov.nist.fhir.adapter.forecaster.ForecasterUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.ImmunizationRecommendation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 *
 * @author mccaffrey
 */
public class ImmunizationRecommendationProvider implements IResourceProvider {
    
    @Override
    public Class<Parameters> getResourceType() {
        return Parameters.class;
    }
    
    @Operation(name="$IR")    
    //public ImmunizationRecommendation getImmunizationRecommendation(@ResourceParam Parameters parameters, HttpServletRequest request, HttpServletResponse response) {
   public Parameters getImmunizationRecommendation(@ResourceParam String theRawBody) {
//             public Parameters getImmunizationRecommendation() {

        System.out.println("===> getImmunizationRecommendation");

        Parameters params = new Parameters();

        FhirContext ctx = FhirContext.forDstu3();
        IBaseResource bodyFhir = ctx.newXmlParser().parseResource(theRawBody);
        
        if(!(bodyFhir instanceof Parameters)) {
            //TODO fail gracefully
            System.out.println("Not Parameters!");
            return null;
        }
        
        Parameters inputParameters = (Parameters) bodyFhir;                        
        System.out.println("POST PARSING = \n" + ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(inputParameters));
        
        Parameters outputParameters = ForecasterUtils.run(inputParameters);
        System.out.println("Sending back = \n" + ctx.newXmlParser().setPrettyPrint(false).encodeResourceToString(outputParameters));
        return outputParameters;
        
        
        
    }
}
