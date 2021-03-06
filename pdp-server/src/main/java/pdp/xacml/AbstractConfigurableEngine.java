package pdp.xacml;

import org.apache.openaz.xacml.api.Attribute;
import org.apache.openaz.xacml.api.AttributeValue;
import org.apache.openaz.xacml.api.Identifier;
import org.apache.openaz.xacml.api.pip.PIPException;
import org.apache.openaz.xacml.api.pip.PIPFinder;
import org.apache.openaz.xacml.api.pip.PIPRequest;
import org.apache.openaz.xacml.api.pip.PIPResponse;
import org.apache.openaz.xacml.std.IdentifierImpl;
import org.apache.openaz.xacml.std.StdAttribute;
import org.apache.openaz.xacml.std.StdAttributeValue;
import org.apache.openaz.xacml.std.StdStatus;
import org.apache.openaz.xacml.std.StdStatusCode;
import org.apache.openaz.xacml.std.pip.StdMutablePIPResponse;
import org.apache.openaz.xacml.std.pip.StdPIPRequest;
import org.apache.openaz.xacml.std.pip.StdSinglePIPResponse;
import org.apache.openaz.xacml.std.pip.engines.ConfigurableEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import pdp.stats.StatsContext;
import pdp.stats.StatsContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.util.stream.Collectors.toList;
import static pdp.xacml.PdpPolicyDefinitionParser.NAME_ID;

public abstract class AbstractConfigurableEngine implements ConfigurableEngine {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    protected PIPRequest requiredAttribute;
    protected PIPRequest providedAttribute;

    protected PIPResponse empty;
    protected PIPResponse missingNameId;

    //we need to keep track of performance

    @Override
    public void configure(String id, Properties properties) throws PIPException {
        IdentifierImpl identifierDataType = new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string");
        IdentifierImpl attributeCategory = new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");

        requiredAttribute = requiredAttribute(identifierDataType, attributeCategory);

        IdentifierImpl identifierAttribute = new IdentifierImpl(getIdentifierProvidedAttribute());
        providedAttribute = providedAttribute(identifierDataType, attributeCategory, identifierAttribute);

        Attribute attribute = new StdAttribute(attributeCategory, identifierAttribute, Collections.emptyList(), null, true);
        empty = new StdSinglePIPResponse(attribute);
        missingNameId = new StdMutablePIPResponse(new StdStatus(StdStatusCode.STATUS_CODE_MISSING_ATTRIBUTE, NAME_ID + " attribute missing"));

    }

    protected StdPIPRequest providedAttribute(IdentifierImpl identifierDataType, IdentifierImpl attributeCategory, IdentifierImpl identifierAttribute) {
        return new StdPIPRequest(attributeCategory, identifierAttribute, identifierDataType);
    }

    protected StdPIPRequest requiredAttribute(IdentifierImpl identifierDataType, IdentifierImpl attributeCategory) {
        return new StdPIPRequest(attributeCategory, new IdentifierImpl(NAME_ID), identifierDataType);
    }

    @Override
    public Collection<PIPRequest> attributesRequired() {
        return Arrays.asList(requiredAttribute);
    }

    @Override
    public Collection<PIPRequest> attributesProvided() {
        return Arrays.asList(providedAttribute);
    }

    @Override
    public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
        if (!getIdentifierProvidedAttribute().equals(pipRequest.getAttributeId().getUri().toString())) {
            //this PIP requires a PIP dependent rule to be present in the Policy
            return empty;
        }
        PIPResponse matchingAttributes = pipFinder.getMatchingAttributes(requiredAttribute, this);
        Optional<Attribute> nameAttributeOptional = matchingAttributes.getAttributes().stream().findFirst();
        if (!nameAttributeOptional.isPresent()) {
            return missingNameId;
        }
        Attribute nameAttribute = nameAttributeOptional.get();
        Collection<AttributeValue<?>> values = nameAttribute.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return missingNameId;
        }
        String userUrn = (String) values.stream().findFirst().get().getValue();

        StatsContext stats = StatsContextHolder.getContext();
        long start = System.currentTimeMillis();

        List<Object> result = getAttributes(userUrn);

        long ms = System.currentTimeMillis() - start;
        stats.addPipResponse(getName(), ms);
        LOG.info("{} PIP response for {} took {} ms", getName(), userUrn, ms);

        if (CollectionUtils.isEmpty(result)) {
            return empty;
        }
        Identifier dataTypeId = providedAttribute.getDataTypeId();
        List<AttributeValue<?>> stdAttributeValues = result.stream().map(valueIn -> new StdAttributeValue<>(dataTypeId, valueIn)).collect(toList());
        Attribute responseAttr = new StdAttribute(providedAttribute.getCategory(), providedAttribute.getAttributeId(), stdAttributeValues, null, true);
        LOG.debug("Returning result from PIP {}: {}", getName(), result);
        return new StdSinglePIPResponse(responseAttr);
    }

    protected abstract List<Object> getAttributes(String userUrn);

    public abstract String getIdentifierProvidedAttribute();
}
