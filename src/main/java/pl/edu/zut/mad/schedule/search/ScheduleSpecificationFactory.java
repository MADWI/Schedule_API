package pl.edu.zut.mad.schedule.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.edu.zut.mad.schedule.exception.ScheduleExceptionFactory;
import pl.edu.zut.mad.schedule.model.inner.Schedule;
import pl.edu.zut.mad.schedule.model.inner.Schedule.Field;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static pl.edu.zut.mad.schedule.model.inner.Schedule.Field.DATE_FROM;
import static pl.edu.zut.mad.schedule.model.inner.Schedule.Field.DATE_TO;
import static pl.edu.zut.mad.schedule.search.ScheduleSpecification.DATE_PATTERN;

@Component
public class ScheduleSpecificationFactory {

    private final ScheduleExceptionFactory exceptionFactory;

    @Autowired
    public ScheduleSpecificationFactory(ScheduleExceptionFactory exceptionFactory) {
        this.exceptionFactory = exceptionFactory;
    }

    public Optional<Specification<Schedule>> specification(Map<String, String> params) {
        if (!isDateRangeValid(params)) {
            throw exceptionFactory.invalidDateRange();
        }

        List<ScheduleSpecification> scheduleSpecifications = params.entrySet()
                .stream()
                .filter(this::isValidParam)
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new SearchCriteria(e.getKey(), e.getValue()))
                .map(ScheduleSpecification::new)
                .collect(toList());

        Specification<Schedule> specification = null;
        if (!scheduleSpecifications.isEmpty()) {
            specification = scheduleSpecifications.get(0);
        }

        for (int i = 1; i < scheduleSpecifications.size(); i++) {
            specification = Specification.where(specification)
                    .and(scheduleSpecifications.get(i));
        }

        return Optional.ofNullable(specification);
    }

    public Specification<Schedule> specification(Map<String, String> params, List<Integer> groupIds) {
        Optional<Specification<Schedule>> optionalSpecification = specification(params);
        return optionalSpecification.map(scheduleSpecification -> addGroupSpecification(scheduleSpecification, groupIds))
                .orElseGet(() -> getGroupSpecification(groupIds));
    }

    private Specification<Schedule> addGroupSpecification(Specification<Schedule> scheduleSpecification, List<Integer> groupIds) {
        return Specification.where(scheduleSpecification)
                .and(new GroupScheduleSpecification(groupIds));
    }

    private Specification<Schedule> getGroupSpecification(List<Integer> groupIds) {
        return Specification.where(new GroupScheduleSpecification(groupIds));
    }

    private boolean isDateRangeValid(Map<String, String> params) {
        final String from = params.get(DATE_FROM.getKey());
        final String to = params.get(DATE_TO.getKey());

        if (!StringUtils.isEmpty(from) && !StringUtils.isEmpty(to)) {
            try {
                final LocalDate dateFrom = LocalDate.parse(from, DateTimeFormatter.ofPattern(DATE_PATTERN));
                final LocalDate dateTo = LocalDate.parse(to, DateTimeFormatter.ofPattern(DATE_PATTERN));
                return dateFrom.compareTo(dateTo) <= 0;
            } catch (DateTimeParseException exception) {
                throw ScheduleExceptionFactory.badRequest(exception.getMessage());
            }
        }

        return true;
    }

    private boolean isValidParam(Map.Entry<String, String> e) {
        return Arrays.stream(Field.values())
                .anyMatch(f -> f.getKey().equals(e.getKey()));
    }
}
