package se.crisp.codekvast.server.codekvast_server.messagehandler;

import com.google.common.eventbus.EventBus;
import lombok.Value;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for sending signatures to the correct users.
 *
 * @author Olle Hallin
 */
@Service
@Slf4j
public class SignatureHandler extends AbstractMessageHandler {
    private final UserHandler userHandler;

    @Inject
    public SignatureHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserHandler userHandler) {
        super(eventBus, messagingTemplate);
        this.userHandler = userHandler;
    }

    /**
     * The JavaScript layer starts a STOMP subscription for signatures.
     *
     * @param principal The identity of the authenticated user.
     * @return The current FilterValues that the user shall use.
     */
    @SubscribeMapping("/signatures")
    public Signatures subscribeSignatures(Principal principal) {
        String username = principal.getName();
        log.debug("'{}' is subscribing to signatures", username);

        return getSignatures(username);
    }

    @Value
    @Builder
    static class Signatures {
        private final List<Signature> signatures;
        private final Set<String> packages;
    }

    @Value
    @Builder
    static class Signature {
        private final String name;
        private final long invokedAtMillis;
        private final String invokedAtString;
        private final String age;
    }

    //---- Fake stuff below ---------------------------

    static Signatures getSignatures(String username) {
        List<Signature> signatures = new ArrayList<>();
        Set<String> packages = new TreeSet<>();

        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Pattern pkgPattern = Pattern.compile("([\\p{javaLowerCase}\\.]+)\\.");

        for (String s : fakeSignatures) {
            Matcher m = pkgPattern.matcher(s);
            if (m.find()) {
                String pkg = m.group(1);
                packages.add(pkg);
            }

            long invokedAtMillis = getRandomInvokedAtMillis(now);
            String invokedAtString = invokedAtMillis == 0L ? "" : sdf.format(new Date(invokedAtMillis));
            String age = getAge(now, invokedAtMillis);

            signatures.add(Signature.builder()
                                    .name(s)
                                    .invokedAtMillis(invokedAtMillis)
                                    .invokedAtString(invokedAtString)
                                    .age(age)
                                    .build());
        }

        Set<String> parentPackages = new HashSet<String>();
        for (String pkg : packages) {
            int dot = pkg.lastIndexOf('.');
            if (dot > 0) {
                String parentPkg = pkg.substring(0, dot);
                parentPackages.add(parentPkg);
            }
        }
        packages.addAll(parentPackages);
        return Signatures.builder().signatures(signatures).packages(packages).build();
    }

    static String getAge(long now, long invokedAtMillis) {
        if (invokedAtMillis == 0L) {
            return "";
        }

        long age = now - invokedAtMillis;

        long minutes = 60 * 1000L;
        if (age < 60 * minutes) {
            return String.format("%d min", age / minutes);
        }

        long hours = minutes * 60;
        if (age < 24 * hours) {
            return String.format("%d hours", age / hours);
        }
        long days = hours * 24;
        if (age < 30 * days) {
            return String.format("%d days", age / days);
        }

        long week = days * 7;
        return String.format("%d weeks", age / week);
    }

    static long getRandomInvokedAtMillis(long now) {
        // 20% probability of not being invoked at all
        if (random.nextInt(100) < 20) {
            return 0L;
        }

        // uniformly random between 2 weeks back in time and now
        int twoWeeksInMillis = 2 * 7 * 24 * 60 * 60 * 1000;
        return now - random.nextInt(twoWeeksInMillis);
    }

    /**
     * Fake way to see that STOMP updates work.
     */
    @Scheduled(fixedRate = 5000L)
    public void sendSignaturesToActiveUsers() {
        for (String username : userHandler.getActiveUsernames()) {
            Signatures sig = getSignatures(username);

            log.debug("Sending {} signatures and {} packages to '{}'", sig.getSignatures().size(), sig.getPackages().size(), username);
            messagingTemplate.convertAndSendToUser(username, "/queue/signatures", sig);
        }
    }

    private static Random random = new Random();

    private static final String[] fakeSignatures = {
            //@formatter:off
        "se.su.dsv.exia.services.AnswerService.hasAnswerWithoutPoints(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AnswerService.setComment(se.su.dsv.exia.domain.Answer, java.lang.String)",
        "se.su.dsv.exia.services.AssessmentDelegationService.deleteByExamination(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AssessmentDelegationService.findAvailableExaminations(java.lang.String)",
        "se.su.dsv.exia.services.AssessmentDelegationService.findDelegations(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AssessmentDelegationService.giveAssessmentPrivileges(java.lang.String, se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AssessmentDelegationService.removeAssessmentPrivileges(se.su.dsv.exia.domain.AssessmentPrivilege)",
        "se.su.dsv.exia.services.AssessmentDelegationService.verifyAccess(java.lang.String, se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.AssessmentProtocolService.createProtocol(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AssessmentService.countAssessed(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.AssessmentService.countNotAssessedQuestions(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AssessmentService.getPointsAssigned(se.su.dsv.exia.domain.Attempt, se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.AssessmentService.hasPointsAssigned(se.su.dsv.exia.domain.Attempt, se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.AttemptService.countNotAssessedStudents(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.countNumberOfHandedInBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.countNumberOfNotHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.countStudents(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.find(java.lang.Long)",
        "se.su.dsv.exia.services.AttemptService.findAttempt(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.AttemptService.findFinishedNotHandedOutHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findGradedNotHandedOut(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findGradedNotHandedOutAlwaysIncludeBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findHandedInNotBlankAttempts(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findHandedOut(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findHandedOutByPersonnummer(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.AttemptService.findLatestAttempt(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.AttemptService.findNotGradedAndNotBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findNotGradedAndNotBlankPersonnummer(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.findNotHandedOut(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.AttemptService.findNumberOfHandedInNotBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.getNextAttempt(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.AttemptService.getPreviousAttempt(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.AttemptService.handOut(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.AttemptService.initializeFindNotGradedAndNotBlankPaged(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.AttemptService.startExaminationAttempt(se.su.dsv.exia.domain.Personnummer, se.su.dsv.exia.domain" +
                    ".Examination)",
        "se.su.dsv.exia.services.AuthorizationService.isAuthorized(java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.DateService.now()",
        "se.su.dsv.exia.services.ExaminationService.checkAccessCode(se.su.dsv.exia.domain.Examination, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationService.deleteIfItHasNotStarted(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.ExaminationService.find(java.lang.Long)",
        "se.su.dsv.exia.services.ExaminationService.findAll(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.ExaminationService.findAllScheduledExaminations()",
        "se.su.dsv.exia.services.ExaminationService.findAvailableExaminationsForPersonnummer(se.su.dsv.exia.domain.Personnummer, boolean)",
        "se.su.dsv.exia.services.ExaminationService.findExaminationsScheduledForTheFuture(java.lang.String)",
        "se.su.dsv.exia.services.ExaminationService.findOngoingExaminationsForPersonnummer(se.su.dsv.exia.domain.Personnummer, boolean)",
        "se.su.dsv.exia.services.ExaminationService.findScheduledPastExaminations(java.lang.String)",
        "se.su.dsv.exia.services.ExaminationService.setAccessCode(se.su.dsv.exia.domain.Examination, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationService.setAssessmentDone(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.ExaminationTestService.copy(se.su.dsv.exia.domain.ExaminationTest, java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationTestService.createTest(java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationTestService.find(java.lang.Long)",
        "se.su.dsv.exia.services.ExaminationTestService.findAll()",
        "se.su.dsv.exia.services.ExaminationTestService.findUnscheduledTests(java.lang.String)",
        "se.su.dsv.exia.services.ExaminationTestService.updateName(se.su.dsv.exia.domain.ExaminationTest, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationTestService.updatePreface(se.su.dsv.exia.domain.ExaminationTest, java.lang.String)",
        "se.su.dsv.exia.services.ExaminationTestService.updateRandomChoiceOrder(se.su.dsv.exia.domain.ExaminationTest, java.lang.Boolean)",
        "se.su.dsv.exia.services.ExaminationTestService.updateRandomOrder(se.su.dsv.exia.domain.ExaminationTest, java.lang.Boolean)",
        "se.su.dsv.exia.services.GradingService.getGrade(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.HandInService.handIn(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.HandInService.handInBlank(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.QuestionService.addChoice(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.createSubLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.createTopLevel(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.delete(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.deleteByExaminationTest(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.deleteChoice(se.su.dsv.exia.domain.Question, java.lang.String)",
        "se.su.dsv.exia.services.QuestionService.find(java.lang.Long)",
        "se.su.dsv.exia.services.QuestionService.findAll(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.findAllAnswerable(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.findByParent(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.findByParent(se.su.dsv.exia.domain.Question, java.lang.Long)",
        "se.su.dsv.exia.services.QuestionService.findChoiceQuestions(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.findChoices(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.findTopLevelQuestion(se.su.dsv.exia.domain.ExaminationTest, java.lang.Long)",
        "se.su.dsv.exia.services.QuestionService.findTopLevelQuestions(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.findTopLevelQuestionsInPresentationOrder(se.su.dsv.exia.domain.ExaminationTest, java" +
                    ".lang.String)",
        "se.su.dsv.exia.services.QuestionService.getFirst(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.getNextAtTheSameLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.getNumber(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.getPreviousAtTheSameLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.hasNext(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.hasPrevious(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.hasSubQuestions(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.initializeFindTopLevelQuestionsPaged(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.QuestionService.insertAfter(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.moveDown(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.moveUp(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.save(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.QuestionService.save(se.su.dsv.exia.domain.Question, java.lang.String, java.lang.String, boolean, java" +
                    ".lang.String)",
        "se.su.dsv.exia.services.SchedulingService.canBeStarted(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.SchedulingService.isEditable(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.SchedulingService.isEditable(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.SchedulingService.validLatestStartTime(java.util.Date, java.util.Date)",
        "se.su.dsv.exia.services.SchedulingService.validStartTime(java.util.Date)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.1.compare(java.lang.Object, java.lang.Object)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.1.compare(se.su.dsv.exia.domain.Answer, se.su.dsv.exia.domain.Answer)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.2.compare(java.lang.Object, java.lang.Object)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.2.compare(se.su.dsv.exia.domain.Answer, se.su.dsv.exia.domain.Answer)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.3.compare(java.lang.Object, java.lang.Object)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.3.compare(se.su.dsv.exia.domain.Answer, se.su.dsv.exia.domain.Answer)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.assignPoints(se.su.dsv.exia.domain.Answer, java.lang.Integer, java.lang.String)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.calculateCurrentTotalPoints(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.calculatePoints(se.su.dsv.exia.domain.Attempt, se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.getFirstNotAssessedAnswerOrNull(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.getFirstNotAssessedStudentOrNull(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.getHistory(se.su.dsv.exia.domain.Question, se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.getNumberOfAssessedAnswers(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.hasAnswerWithoutPoints(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AnswerServiceImpl.setComment(se.su.dsv.exia.domain.Answer, java.lang.String)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.1.compare(java.lang.Object, java.lang.Object)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.1.compare(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain" +
                    ".Examination)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.deleteByExamination(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.findAvailableExaminations(java.lang.String)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.findDelegations(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.giveAssessmentPrivileges(java.lang.String, se.su.dsv.exia.domain" +
                    ".Examination)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.removeAssessmentPrivileges(se.su.dsv.exia.domain" +
                    ".AssessmentPrivilege)",
        "se.su.dsv.exia.services.impl.AssessmentDelegationServiceImpl.verifyAccess(java.lang.String, se.su.dsv.exia.domain" +
                    ".ExaminationTest)",
        "se.su.dsv.exia.services.impl.AssessmentProtocolServiceImpl.createProtocol(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AssessmentServiceImpl.countAssessed(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AssessmentServiceImpl.countNotAssessedQuestions(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AssessmentServiceImpl.getPointsAssigned(se.su.dsv.exia.domain.Attempt, se.su.dsv.exia.domain" +
                    ".Question)",
        "se.su.dsv.exia.services.impl.AssessmentServiceImpl.hasPointsAssigned(se.su.dsv.exia.domain.Attempt, se.su.dsv.exia.domain" +
                    ".Question)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.countNotAssessedStudents(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.countNumberOfHandedInBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.countNumberOfNotHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.countStudents(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.find(java.lang.Long)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findAttempt(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain" +
                    ".Personnummer)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findFinishedNotHandedOutHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findGradedNotHandedOut(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findGradedNotHandedOutAlwaysIncludeBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findHandedIn(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findHandedInNotBlankAttempts(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findHandedOut(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findHandedOutByPersonnummer(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findLatestAttempt(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findNotGradedAndNotBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findNotGradedAndNotBlankPersonnummer(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findNotHandedOut(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.findNumberOfHandedInNotBlank(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.getGrade(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.getNextAttempt(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.getPreviousAttempt(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.handOut(se.su.dsv.exia.domain.Attempt)",
        "se.su.dsv.exia.services.impl.AttemptServiceImpl.initializeFindNotGradedAndNotBlankPaged(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.AuthorizationServiceImpl.isAuthorized(java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.checkAccessCode(se.su.dsv.exia.domain.Examination, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.deleteIfItHasNotStarted(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.find(java.lang.Long)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findAll(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findAllScheduledExaminations()",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findAvailableExaminationsForPersonnummer(se.su.dsv.exia.domain.Personnummer," +
                    " boolean)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findExaminationsScheduledForTheFuture(java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findOngoingExaminationsForPersonnummer(se.su.dsv.exia.domain.Personnummer, " +
                    "boolean)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.findScheduledPastExaminations(java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.setAccessCode(se.su.dsv.exia.domain.Examination, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationServiceImpl.setAssessmentDone(se.su.dsv.exia.domain.Examination)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.copy(se.su.dsv.exia.domain.ExaminationTest, java.lang.String, java.lang" +
                    ".String)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.createTest(java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.find(java.lang.Long)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.findAll()",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.findMyExaminationTests()",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.findUnscheduledTests(java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.updateName(se.su.dsv.exia.domain.ExaminationTest, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.updatePreface(se.su.dsv.exia.domain.ExaminationTest, java.lang.String)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.updateRandomChoiceOrder(se.su.dsv.exia.domain.ExaminationTest, java.lang" +
                    ".Boolean)",
        "se.su.dsv.exia.services.impl.ExaminationTestServiceImpl.updateRandomOrder(se.su.dsv.exia.domain.ExaminationTest, java.lang" +
                    ".Boolean)",
        "se.su.dsv.exia.services.impl.HandInServiceImpl.handIn(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.HandInServiceImpl.handInBlank(se.su.dsv.exia.domain.Examination, se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.services.impl.MockAuthorizationService.isAuthorized(java.lang.String, java.lang.String)",
        "se.su.dsv.exia.services.impl.MockAuthorizationService.setAuthorized(boolean)",
        "se.su.dsv.exia.services.impl.OverridableDateServiceImpl.now()",
        "se.su.dsv.exia.services.impl.OverridableDateServiceImpl.setOverride(java.util.Date)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.addChoice(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.createSubLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.createTopLevel(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.delete(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.deleteByExaminationTest(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.deleteChoice(se.su.dsv.exia.domain.Question, java.lang.String)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.find(java.lang.Long)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findAll(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findAllAnswerable(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findByParent(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findByParent(se.su.dsv.exia.domain.Question, java.lang.Long)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findChoiceQuestions(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findChoices(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findTopLevelQuestion(se.su.dsv.exia.domain.ExaminationTest, java.lang.Long)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.findTopLevelQuestions(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.getFirst(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.getNextAtTheSameLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.getNumber(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.getPreviousAtTheSameLevel(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.hasNext(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.hasPrevious(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.hasSubQuestions(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.initializeFindTopLevelQuestionsPaged(se.su.dsv.exia.domain.ExaminationTest)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.insertAfter(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.moveDown(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.moveUp(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.QuestionServiceImpl.save(se.su.dsv.exia.domain.Question)",
        "se.su.dsv.exia.services.impl.SchedulingServiceImpl.validLatestStartTime(java.util.Date, java.util.Date)",
        "se.su.dsv.exia.services.impl.SchedulingServiceImpl.validStartTime(java.util.Date)",
        "se.su.dsv.exia.view.ExceptionHandler.onException(org.apache.wicket.request.cycle.RequestCycle, java.lang.Exception)",
        "se.su.dsv.exia.view.ExiaSession.get()",
        "se.su.dsv.exia.view.ExiaSession.getAttemptId()",
        "se.su.dsv.exia.view.ExiaSession.getEditingMode()",
        "se.su.dsv.exia.view.ExiaSession.getRemoteUser()",
        "se.su.dsv.exia.view.ExiaSession.isLoggedIn()",
        "se.su.dsv.exia.view.ExiaSession.logIn(se.su.dsv.exia.domain.Personnummer)",
        "se.su.dsv.exia.view.ExiaSession.logOut()",
        "se.su.dsv.exia.view.ExiaSession.loggedInAsOrDoLogin()",
        "se.su.dsv.exia.view.ExiaSession.setEditingMode(java.lang.Class)",
        "se.su.dsv.exia.view.ExiaSession.setRemoteUser(java.lang.String)",
        "se.su.dsv.exia.view.ExiaSession.startExamination(java.lang.Long)",
        "se.su.dsv.exia.view.components.EventDelegatingTinyMceBehavior.bind(org.apache.wicket.Component)",
        "se.su.dsv.exia.view.components.GoToLink.onAfterSubmit()",
        "se.su.dsv.exia.view.components.OppositeVisibility.onConfigure(org.apache.wicket.Component)",
        "se.su.dsv.exia.view.components.PdfResource.1.getContentType()",
        "se.su.dsv.exia.view.components.PdfResource.1.write(java.io.OutputStream)",
        "se.su.dsv.exia.view.components.PdfResource.isViewable()",
        "se.su.dsv.exia.view.components.QuestionNavigation.Direction.valueOf(java.lang.String)",
        "se.su.dsv.exia.view.components.QuestionNavigation.Direction.values()",
        "se.su.dsv.exia.view.components.QuestionNavigation.onAfterSubmit()",
        "se.su.dsv.exia.view.components.QuestionTitle.getQuestionTitle()",
        "se.su.dsv.exia.view.components.QuestionTitleAsHtml.getQuestionTitle()",
        "se.su.dsv.exia.view.components.RadioChoiceGroup.getChoiceLetter(int)",
        "se.su.dsv.exia.view.components.WordCountingTextArea.addLabel(org.apache.wicket.markup.html.basic.Label)",
        "se.su.dsv.exia.view.components.WordCountingTextArea.saveAnswer()",
        "se.su.dsv.exia.view.components.WordCountingTextArea.update(org.apache.wicket.ajax.AjaxRequestTarget)",
        "se.su.dsv.exia.view.converter.PersonnummerConverter.convertToObject(java.lang.String, java.util.Locale)",
        "se.su.dsv.exia.view.converter.PersonnummerConverter.convertToString(java.lang.Object, java.util.Locale)",
        "se.su.dsv.exia.view.converter.PersonnummerConverter.convertToString(se.su.dsv.exia.domain.Personnummer, java.util.Locale)",
        "se.su.dsv.exia.view.models.AnswerTextModel.getExamination()",
        "se.su.dsv.exia.view.models.AnswerTextModel.getQuestion()",
        "se.su.dsv.exia.view.pages.ExiaErrorPage.isErrorPage()",
        "se.su.dsv.exia.view.pages.ExiaErrorPage.isVersioned()",
        "se.su.dsv.exia.view.panel.ChoiceQuestionPanel.3.onClick()",
        "se.su.dsv.exia.view.panel.LanguagePanel.FlagLink.onClick()",
        "se.su.dsv.exia.view.panel.WordCountContainer.visitFeedbackPanel(org.apache.wicket.ajax.AjaxRequestTarget)",
        "se.su.dsv.exia.view.panel.WordCountContainer.visitWordCountingTextAreas(org.apache.wicket.ajax.AjaxRequestTarget)",
        "se.su.dsv.exia.view.panel.WordCountingAnswerPanel.AutoSavingBehavior.getCallbackUrl()",
        "se.su.dsv.exia.view.util.ExiaUtil.setLanguage(org.apache.wicket.request.mapper.parameter.PageParameters)",
        //@formatter:on
    };
}
