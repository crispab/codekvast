package io.codekvast.dashboard.file_import.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** @author olle.hallin@crisp.se */
@RequiredArgsConstructor
public class SyntheticSignatureServiceTest {

  @Mock private SyntheticSignatureDAO dao;

  private SyntheticSignatureService syntheticSignatureService;

  @BeforeEach
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);
    when(dao.getPatterns()).thenReturn(Collections.emptyList());

    syntheticSignatureService = new SyntheticSignatureService(dao);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {"true; foo", "true; bar", "false; baz"})
  public void should_compose_patterns(boolean expected, String signature) {
    // given
    SyntheticSignaturePattern p1 =
        SyntheticSignaturePattern.builder().id(1L).pattern(".*foo.*").build();
    SyntheticSignaturePattern p2 =
        SyntheticSignaturePattern.builder().id(2L).pattern(".*bar.*").build();
    when(dao.getPatterns()).thenReturn(asList(p1, p2));

    // when
    boolean isSyntheticMethod = syntheticSignatureService.isSyntheticMethod(signature);

    // then
    assertThat(isSyntheticMethod, is(expected));
    verify(dao, never()).rejectPattern(any(SyntheticSignaturePattern.class), anyString());
  }

  @Test
  public void should_reject_bad_patterns() {
    // given
    SyntheticSignaturePattern p1 =
        SyntheticSignaturePattern.builder().id(1L).pattern(".*foo.*(").build();
    SyntheticSignaturePattern p2 =
        SyntheticSignaturePattern.builder().id(2L).pattern(".*bar.*").build();
    when(dao.getPatterns()).thenReturn(asList(p1, p2));

    // when, then
    assertThat(syntheticSignatureService.isSyntheticMethod("foo"), is(false));
    assertThat(syntheticSignatureService.isSyntheticMethod("bar"), is(true));
    verify(dao).rejectPattern(eq(p1), anyString());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = ';',
      value = {
        "false; foo.()", // Constructor of Scala companion obj,
        "false; foo..bar()", // Method in Scala companion obj,
        "false; foo.bar$1()", // Anonymous inner cl,
        "false; foo.bar()",
        "true; controllers.Assets.play$api$http$HeaderNames$_setter_$LOCATION_$eq(java.lang.String)",
        "true; controllers.AssetsBuilder..anonfun.at.1..anonfun.apply.2..anonfun.7.apply()",
        "true; controllers.AssetsBuilder.play$api$mvc$Results$_setter_$FailedDependency_$eq(play.api.mvc.Results.Status)",
        "true; controllers.customer1.application.Admin.CONTENT_MD5()",
        "true; controllers.customer1.application.application.play$api$http$HeaderNames$_setter_$IF_RANGE_$eq(java.lang.String)",
        "true; controllers.customer1.application.applicationOpen.play$api$mvc$Results$_setter_$FailedDependency_$eq(play.api.mvc.Results.Status)",
        "true; controllers.customer1.application.Authenticate.play$api$http$HeaderNames$_setter_$CONTENT_MD5_$eq(java.lang.String)",
        "true; controllers.customer1.application.ContentFeed.play$api$http$HeaderNames$_setter_$IF_NONE_MATCH_$eq(java.lang.String)",
        "true; controllers.customer1.application.EscenicFeed.play$api$http$HeaderNames$_setter_$ACCEPT_$eq(java.lang.String)",
        "true; controllers.customer1.application.EscenicFeed.play$api$http$HttpProtocol$_setter_$HTTP_1_0_$eq(java.lang.String)",
        "true; controllers.customer1.application.EscenicFeed.play$api$http$Status$_setter_$OK_$eq(int)",
        "true; controllers.customer1.application.healthgraph.HealthGraphConnector.play$api$http$Status$_setter_$RESET_CONTENT_$eq(int)",
        "true; controllers.customer1.application.healthgraph.HealthGraphMock.play$api$mvc$Results$_setter_$MethodNotAllowed_$eq(play.api.mvc.Results.Status)",
        "true; controllers.customer1.application.LogService.play$api$http$HeaderNames$_setter_$ALLOW_$eq(java.lang.String)",
        "true; controllers.customer1.application.Mobileapplication.play$api$http$HeaderNames$_setter_$AGE_$eq(java.lang.String)",
        "true; controllers.customer1.application.SmpFeed.play$api$http$Status$_setter_$FAILED_DEPENDENCY_$eq(int)",
        "true; controllers.customer1.application.SmpFeed.play$api$mvc$Results$_setter_$MethodNotAllowed_$eq(play.api.mvc.Results.Status)",
        "true; controllers.customer1.application.XxxYyy.$amp()",
        "true; controllers.Default.play$api$http$HeaderNames$_setter_$MAX_FORWARDS_$eq(java.lang.String)",
        "true; controllers.ExternalAssets.play$api$http$HeaderNames$_setter_$AUTHORIZATION_$eq(java.lang.String)",
        "true; customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB$BIND_CALLBACKS(java.lang.Object)",
        "true; customer1.FooConfig..FastClassBySpringCGLIB..73e1cc5a.getIndex(org.springframework.cglib.core.Signature)",
        "true; customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allGuests(se.crisp.signup4.models.Event)",
        "true; customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allMembers(se.crisp.signup4.models.Event)",
        "true; customer2.controllers.EventsSecured..se$crisp$signup4$controllers$EventsSecured$$isReminderToBeSent(jp.t2v.lab.play2.stackc.RequestWithAttributes)",
        "true; customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_AUTHENTICATION_URL()",
        "true; customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_CLIENT_ID$lzycompute()",
        "true; customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_CLIENT_ID()",
        "true; customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$getEmailAddress(java.lang.String)",
        "true; customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$requestAccessToken(java.lang.String, java.lang.String)",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_AUTHENTICATION_URL()",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_ID$lzycompute()",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_ID()",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_SECRET$lzycompute()",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_SECRET()",
        "true; customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_TOKEN_URL()",
        "true; customer2.controllers.Participations..se$crisp$signup4$controllers$Participations$$asLogMessage(se.crisp.signup4.models.Participation, play.api.i18n.Lang)",
        "true; customer2.controllers.UsersSecured..se$crisp$signup4$controllers$UsersSecured$$preventPermissionToBeChanged(se.crisp.signup4.models.User)",
        "true; customer2.controllers.UsersSecured.HTTP_VERSION_NOT_SUPPORTED()",
        "true; customer2.controllers.UsersSecured.NOT_MODIFIED()",
        "true; customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$checkEvents(se.crisp.signup4.models.User)",
        "true; customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$remindParticipant(se.crisp.signup4.models.Event, se.crisp.signup4.models.User, se.crisp.signup4.models.User)",
        "true; customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$remindParticipants(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)",
        "true; customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$createCancellationMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)",
        "true; customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$createReminderMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)",
        "true; customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$sendMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User, scala.Function2)",
        "true; customer2.views.html.memberships.edit..se$crisp$signup4$views$html$memberships$edit$$field$1(java.lang.String, play.api.data.Form)",
        "true; foo$$bar()",
        "true; support.customer1.application.domain.sale.Product.copy$default$8()",
        "true; support.customer1.application.view.magazine.Article.$lessinit$greater$default$10()",
        "true; views.html.customer1.application.export.header.copy$default$1()",
        "true; views.html.defaultpages.devError.copy$default$1()",
        "true; io.codekvast.backoffice.facts.CollectionStarted.canEqual(java.lang.Object)",
      })
  public void should_have_decent_fallbacks(boolean expected, String signature) {
    when(dao.getPatterns()).thenReturn(Collections.emptyList());
    assertThat(syntheticSignatureService.isSyntheticMethod(signature), is(expected));
  }
}
