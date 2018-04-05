package io.codekvast.dashboard.dashboard.impl;

import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class DashboardServiceSyntheticMethodTest {

    @Parameterized.Parameters(name = "{index}: isSynthetic(\"{1}\")={0}")
    public static Object[][] data() {
        return new Object[][]{
            {false, "foo.bar()"},
            {false, "foo.bar$1()"},
            {true, "foo..bar()"},
            {true, "foo$$bar()"},
            {true, "controllers.Assets.play$api$http$HeaderNames$_setter_$LOCATION_$eq(java.lang.String)"},
            {true, "controllers.AssetsBuilder.play$api$mvc$Results$_setter_$FailedDependency_$eq(play.api.mvc.Results.Status)"},
            {true, "controllers.Default.play$api$http$HeaderNames$_setter_$MAX_FORWARDS_$eq(java.lang.String)"},
            {true, "controllers.ExternalAssets.play$api$http$HeaderNames$_setter_$AUTHORIZATION_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.application.play$api$http$HeaderNames$_setter_$IF_RANGE_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.applicationOpen.play$api$mvc$Results$_setter_$FailedDependency_$eq(play.api.mvc.Results.Status)"},
            {true, "controllers.customer1.application.Authenticate.play$api$http$HeaderNames$_setter_$CONTENT_MD5_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.ContentFeed.play$api$http$HeaderNames$_setter_$IF_NONE_MATCH_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.EscenicFeed.play$api$http$HeaderNames$_setter_$ACCEPT_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.EscenicFeed.play$api$http$HttpProtocol$_setter_$HTTP_1_0_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.EscenicFeed.play$api$http$Status$_setter_$OK_$eq(int)"},
            {true, "controllers.customer1.application.healthgraph.HealthGraphConnector.play$api$http$Status$_setter_$RESET_CONTENT_$eq(int)"},
            {true, "controllers.customer1.application.healthgraph.HealthGraphMock.play$api$mvc$Results$_setter_$MethodNotAllowed_$eq(play.api.mvc.Results.Status)"},
            {true, "controllers.customer1.application.LogService.play$api$http$HeaderNames$_setter_$ALLOW_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.Mobileapplication.play$api$http$HeaderNames$_setter_$AGE_$eq(java.lang.String)"},
            {true, "controllers.customer1.application.SmpFeed.play$api$http$Status$_setter_$FAILED_DEPENDENCY_$eq(int)"},
            {true, "controllers.customer1.application.SmpFeed.play$api$mvc$Results$_setter_$MethodNotAllowed_$eq(play.api.mvc.Results.Status)"},
            {true, "controllers.customer1.application.Admin.CONTENT_MD5()"},
            {true, "controllers.customer1.application.XxxYyy.()"},
            {true, "controllers.customer1.application.XxxYyy.$amp()"},
            {true, "customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allGuests(se.crisp.signup4.models.Event)"},
            {true, "customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allMembers(se.crisp.signup4.models.Event)"},
            {true, "customer2.controllers.EventsSecured..se$crisp$signup4$controllers$EventsSecured$$isReminderToBeSent(jp.t2v.lab.play2.stackc.RequestWithAttributes)"},
            {true, "customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_AUTHENTICATION_URL()"},
            {true, "customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_CLIENT_ID$lzycompute()"},
            {true, "customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$FACEBOOK_CLIENT_ID()"},
            {true, "customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$getEmailAddress(java.lang.String)"},
            {true, "customer2.controllers.FacebookAuth..se$crisp$signup4$controllers$FacebookAuth$$requestAccessToken(java.lang.String, java.lang.String)"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_AUTHENTICATION_URL()"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_ID$lzycompute()"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_ID()"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_SECRET$lzycompute()"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_CLIENT_SECRET()"},
            {true, "customer2.controllers.GoogleAuth..se$crisp$signup4$controllers$GoogleAuth$$GOOGLE_TOKEN_URL()"},
            {true, "customer2.controllers.Participations..se$crisp$signup4$controllers$Participations$$asLogMessage(se.crisp.signup4.models.Participation, play.api.i18n.Lang)"},
            {true, "customer2.controllers.UsersSecured..se$crisp$signup4$controllers$UsersSecured$$preventPermissionToBeChanged(se.crisp.signup4.models.User)"},
            {true, "customer2.controllers.UsersSecured.HTTP_VERSION_NOT_SUPPORTED()"},
            {true, "customer2.controllers.UsersSecured.NOT_MODIFIED()"},
            {true, "customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$checkEvents(se.crisp.signup4.models.User)"},
            {true, "customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$remindParticipant(se.crisp.signup4.models.Event, se.crisp.signup4.models.User, se.crisp.signup4.models.User)"},
            {true, "customer2.services.EventReminderActor.se$crisp$signup4$services$EventReminderActor$$remindParticipants(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)"},
            {true, "customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$createCancellationMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)"},
            {true, "customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$createReminderMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User)"},
            {true, "customer2.services.MailReminder..se$crisp$signup4$services$MailReminder$$sendMessage(se.crisp.signup4.models.Event, se.crisp.signup4.models.User, scala.Function2)"},
            {true, "customer2.views.html.memberships.edit..se$crisp$signup4$views$html$memberships$edit$$field$1(java.lang.String, play.api.data.Form)"},
            {true, "support.customer1.application.domain.sale.Product.copy$default$8()"},
            {true, "support.customer1.application.view.magazine.Article.$lessinit$greater$default$10()"},
            {true, "views.html.defaultpages.devError.copy$default$1()"},
            {true, "views.html.customer1.application.export.header.copy$default$1()"},
            };
    }

    private final boolean expected;
    
    private final String signature;

    @Test
    public void should_detect_synthetic_method() {
        assertThat(DashboardServiceImpl.isSyntheticMethod(signature), is(expected));
    }

}