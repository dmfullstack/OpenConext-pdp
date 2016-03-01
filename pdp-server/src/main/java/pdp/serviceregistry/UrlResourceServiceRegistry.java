package pdp.serviceregistry;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.springframework.http.HttpStatus.OK;

public class UrlResourceServiceRegistry extends ClassPathResourceServiceRegistry {

  private final String idpRemotePath;
  private final String spRemotePath;

  private final RestTemplate restTemplate = new RestTemplate();
  private final int period;
  private final BasicAuthenticationUrlResource idpUrlResource;
  private final BasicAuthenticationUrlResource spUrlResource;

  public UrlResourceServiceRegistry(
      String username,
      String password,
      String idpRemotePath,
      String spRemotePath,
      int period) throws MalformedURLException {
    super(false);

    this.idpUrlResource = new BasicAuthenticationUrlResource(idpRemotePath, username, password);
    this.spUrlResource = new BasicAuthenticationUrlResource(spRemotePath, username, password);

    this.idpRemotePath = idpRemotePath;
    this.spRemotePath = spRemotePath;
    this.period = period;

    SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
    requestFactory.setConnectTimeout(5 * 1000);

    newScheduledThreadPool(1).scheduleAtFixedRate(this::initializeMetadata, period, period, TimeUnit.MINUTES);
    super.initializeMetadata();
  }

  @Override
  protected Resource getIdpResource() {
    LOG.debug("Fetching IDP metadata entries from {}", idpRemotePath);
    return idpUrlResource;
  }

  @Override
  protected Resource getSpResource() {
    LOG.debug("Fetching SP metadata entries from {}", spRemotePath);
    return spUrlResource;
  }

  @Override
  protected void initializeMetadata() {
    if (spUrlResource.isModified(period) || idpUrlResource.isModified(period) ) {
      super.initializeMetadata();
    } else {
      LOG.debug("Not refreshing SP metadata. Not modified");
    }
  }


}
