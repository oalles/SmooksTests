package es.omarall.smooks;

import java.io.StringWriter;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.milyn.Smooks;
import org.milyn.javabean.binding.xml.XMLBinding;
import org.milyn.payload.JavaResult;
import org.milyn.payload.JavaSource;
import org.milyn.payload.StringResult;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SmooksTestsApplication.class)
public class SmooksTests implements ResourceLoaderAware {

    private Resource xmlData;

    private Resource unmarshallingConfig;
    private Resource marshallingConfig;

    public Lead getLastLead(Resource resource) throws Exception {

        Smooks smooks = new Smooks(resource.getInputStream());
        JavaResult result = new JavaResult();
        smooks.filterSource(new StreamSource(xmlData.getInputStream()), result);

        Leads leads = (Leads) result.getBean("leads");
        Assert.isTrue(leads.getLeads().size() == 3);
        return leads.getLeads().get(2);

    }

    @Test
    public void unmarshalling() throws Exception {

        Lead lead = getLastLead(unmarshallingConfig);
        Assert.isTrue(lead.getCompany().equals("LA Lakers"));
        Assert.isTrue(lead.getEmployeesNumber() == 1000);
    }

    @Test
    public void marshalling() throws Exception {

        JavaSource js = new JavaSource("lead",
                getLastLead(unmarshallingConfig));
        StringResult sr = new StringResult();

        Smooks smooks = new Smooks(marshallingConfig.getInputStream());
        smooks.filterSource(js, sr);

        LoggerFactory.getLogger(getClass()).debug(sr.toString());
        Assert.isTrue(sr.toString().trim().contains("</Leads>"));
    }

    @Test
    public void unmarshallingViaXMLBinding() throws Exception {

        // XMLBinding nacio con idea de, con un unico config file permitir
        // marshalling y unmarshalling. No me vale.

        XMLBinding xmlBinding = new XMLBinding()
                .add(unmarshallingConfig.getInputStream());
        xmlBinding.intiailize();

        // Read the order XML into the Order Object model...
        StringWriter sw = new StringWriter();
        IOUtils.copy(xmlData.getInputStream(), sw);
        Leads leads = xmlBinding.fromXML(sw.toString(), Leads.class);

        Assert.isTrue(leads.getLeads().size() == 3);
        Lead lead = leads.getLeads().get(2);
        Assert.isTrue(lead.getCompany().equals("LA Lakers"));
        Assert.isTrue(lead.getEmployeesNumber() == 1000);

        // Es distinto del original.
        LoggerFactory.getLogger(getClass()).debug(xmlBinding.toXML(leads));

        // Ver en la ayuda: FreeMarker and the Javabean Cartridge
        // El objetivo es construir UN Java object model (un bean) y una vez que
        // lo tengo serializar usando una plantilla Freemarker.
        // OJO: Es util porque para cualquier origen, a traves del java
        // cartridge podria construir el JOM y despues serializar.
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

        xmlData = resourceLoader.getResource("leads.xml");
        unmarshallingConfig = resourceLoader
                .getResource("smooks-unmarshalling.xml");
        marshallingConfig = resourceLoader
                .getResource("smooks-marshalling.xml");
    }

}
