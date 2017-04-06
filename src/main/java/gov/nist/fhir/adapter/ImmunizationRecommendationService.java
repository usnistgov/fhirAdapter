/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author mccaffrey
 */
@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class ImmunizationRecommendationService extends RestfulServer {
    
    private static final long serialVersionUID = 1L;
    
	public ImmunizationRecommendationService() {
		super(FhirContext.forDstu3()); // Support DSTU3
	}
    
    @Override
       protected void initialize() throws ServletException {
           
           System.out.println("===> Initializing FHIR Servlet!");
           
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
      List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
      
      resourceProviders.add(new ImmunizationRecommendationProvider());
      setResourceProviders(resourceProviders);
      
        //  List<Object> plainProviders=new ArrayList<Object>();
    //plainProviders.add(new ImmunizationRecommendationProvider());
    //setPlainProviders(plainProviders);
      
   }
    
}
