package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Project;
import org.fenixedu.academic.domain.onlineTests.DistributedTest;
import org.fenixedu.academic.domain.onlineTests.OnlineTest;
import org.fenixedu.academic.ui.renderers.converters.YearMonthDayConverter;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

import java.time.YearMonth;
import java.util.stream.Collectors;

@Task(englishTitle = "Send daily test and project notifications")
public class SendDailyEvaluationAlerts extends CronTask {
    @Override
    public void runTask() throws Exception {
        final YearMonthDay today = new YearMonthDay();
        final String tests = ExecutionSemester.readActualExecutionSemester().getAssociatedExecutionCoursesSet().stream()
                .flatMap(ec -> ec.getAssociatedEvaluationsSet().stream())
                .filter(e -> e instanceof OnlineTest)
                .map(e -> (OnlineTest) e)
                .map(ot -> ot.getDistributedTest())
                .filter(dt -> dt.getEndDateDateYearMonthDay().equals(today))
                .map(dt -> describe(dt))
                .sorted()
                .collect(Collectors.joining("\n"));

        final String projects = ExecutionSemester.readActualExecutionSemester().getAssociatedExecutionCoursesSet().stream()
                .flatMap(ec -> ec.getAssociatedEvaluationsSet().stream())
                .filter(e -> e instanceof Project)
                .map(e -> (Project) e)
                .filter(p -> p.getProjectEndDateTime().toYearMonthDay().equals(today))
                .map(p -> describe(p))
                .sorted()
                .collect(Collectors.joining("\n"));

        taskLog("%s%n", "Tests:\n" + tests + "\n\nProjects:\n" + projects);

        org.fenixedu.messaging.core.domain.Message.fromSystem()
                .bcc(Group.managers())
                .singleTos("si@tecnico.ulisboa.pt")
                .subject("Testes e Projetos no Fénix " + today.toString("yyyy-MM-dd"))
                .textBody("Tests:\n" + tests + "\n\nProjects:\n" + projects)
                .send();

    }

    private String describe(final DistributedTest dt) {
        final StringBuilder builder = new StringBuilder();
        builder.append(dt.getEndHourDateHourMinuteSecond().toString("HH:mm"));
        builder.append("\t");
        builder.append(dt.getOnlineTest().getAssociatedExecutionCoursesSet().stream()
                .map(ec -> ec.getName())
                .collect(Collectors.joining( ", " )));
        return builder.toString();
    }

    private String describe(final Project p) {
        final StringBuilder builder = new StringBuilder();
        builder.append(p.getProjectEndDateTime().toString("HH:mm"));
        builder.append("\t");
        builder.append(p.getAssociatedExecutionCoursesSet().stream()
                .map(ec -> ec.getName())
                .collect(Collectors.joining( ", " )));
        return builder.toString();
    }

}
