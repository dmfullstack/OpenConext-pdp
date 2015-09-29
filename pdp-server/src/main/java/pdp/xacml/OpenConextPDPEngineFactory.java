package pdp.xacml;

import org.apache.openaz.xacml.api.pdp.PDPEngine;
import org.apache.openaz.xacml.pdp.OpenAZPDPEngine;
import org.apache.openaz.xacml.pdp.OpenAZPDPEngineFactory;
import org.apache.openaz.xacml.pdp.eval.EvaluationContextFactory;
import org.apache.openaz.xacml.util.FactoryException;
import pdp.repositories.PdpPolicyRepository;
import pdp.teams.VootClient;

import java.io.IOException;
import java.util.Properties;

public class OpenConextPDPEngineFactory extends OpenAZPDPEngineFactory {


  @Override
  public PDPEngine newEngine() throws FactoryException {
    return new OpenConextPDPEngine(EvaluationContextFactory.newInstance(), this.getDefaultBehavior(), this.getScopeResolver());
  }

  @Override
  public PDPEngine newEngine(Properties properties) throws FactoryException {
    return new OpenConextPDPEngine(EvaluationContextFactory.newInstance(properties), this.getDefaultBehavior(), this.getScopeResolver());
  }

  public PDPEngine newEngine(PdpPolicyRepository pdpPolicyRepository, VootClient vootClient) throws FactoryException, IOException {
    EvaluationContextFactory evaluationContextFactory = EvaluationContextFactory.newInstance();
    injectDependencies(pdpPolicyRepository, vootClient, evaluationContextFactory);
    return new OpenConextPDPEngine(evaluationContextFactory, this.getDefaultBehavior(), this.getScopeResolver());
  }

  private void injectDependencies(PdpPolicyRepository pdpPolicyRepository, VootClient vootClient, EvaluationContextFactory evaluationContextFactory) {
  /*
   * Need to do this to remain property driven as OpenAZ is designed and be able to inject dependencies
   */
    if (evaluationContextFactory instanceof OpenConextEvaluationContextFactory) {
      OpenConextEvaluationContextFactory factory = (OpenConextEvaluationContextFactory) evaluationContextFactory;
      factory.setPdpPolicyRepository(pdpPolicyRepository);
      factory.setVootClient(vootClient);
    }
  }


}
