package pl.edu.zut.mad.schedule;

import org.springframework.stereotype.Component;
import pl.edu.zut.mad.schedule.model.inner.Schedule;
import pl.edu.zut.mad.schedule.model.outer.Day;
import pl.edu.zut.mad.schedule.model.outer.Lesson;
import pl.edu.zut.mad.schedule.model.outer.Teacher;
import pl.edu.zut.mad.schedule.model.outer.TimeRange;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
class ScheduleMapper {

    List<Day> daysFrom(final List<Schedule> schedule) {
        return schedule.stream()
                .collect(groupingBy(this::epochDayFrom))
                .entrySet()
                .stream()
                .map(this::dayFrom)
                .sorted(comparing(Day::getDate))
                .collect(toList());
    }

    private long epochDayFrom(final Schedule schedule) {
        return LocalDate.parse(schedule.getDate())
                .atStartOfDay()
                .toEpochSecond(OffsetDateTime.now().getOffset());
    }

    private Day dayFrom(final Map.Entry<Long, List<Schedule>> entry) {
        return new Day(entry.getKey(), entry.getValue().stream()
                .map(this::lessonFrom)
                .sorted(comparing(lesson -> lesson.getTimeRange().getFrom()))
                .collect(toList()));
    }

    private Lesson lessonFrom(final Schedule schedule) {
        final Teacher teacher = new Teacher(schedule.getAcademicTitle(),
                schedule.getName(), schedule.getSurname());

        final LocalTime timeFrom = LocalTime.parse(schedule.getTimeFrom());
        final LocalTime timeTo = LocalTime.parse(schedule.getTimeTo());
        final TimeRange timeRange = new TimeRange(timeFrom, timeTo);

        return new Lesson(schedule.getRoom(), schedule.getCourseType(), schedule.getSubject(),
                schedule.getSemester(), schedule.getFaculty(), schedule.getFacultyAbbreviation(),
                schedule.getFieldOfStudy(), schedule.getReservationStatus(), teacher, timeRange);
    }
}
