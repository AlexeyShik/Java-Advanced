package info.kgeorgiy.ja.shik.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {

    private static final String DEFAULT_VALUE = "";

    private static final Comparator<Student> STUDENT_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName).reversed()
            .thenComparing(Student::getId);

    private static <T extends Collection<String>> Comparator<Map.Entry<GroupName, T>> generateGroupComparator(
            final Comparator<Map.Entry<GroupName, T>> keyComparator) {
        return Map.Entry.<GroupName, T>comparingByValue(Comparator
                .comparingInt(Collection::size))
                .thenComparing(keyComparator);
    }

    private static final Comparator<Map.Entry<GroupName, List<String>>> GROUP_GREATER_COMPARATOR =
            generateGroupComparator(Map.Entry.comparingByKey());

    private static final Comparator<Map.Entry<GroupName, Set<String>>> GROUP_LOWER_COMPARATOR =
            generateGroupComparator(Map.Entry.<GroupName, Set<String>>comparingByKey().reversed());

    private static <T, R> R collect(final Stream<T> stream, final Collector<T, ?, R> collector) {
        return stream.collect(collector);
    }

    private static <T, C extends Collection<T>> C toCollection(final Stream<T> stream, final Supplier<C> constructor) {
        return collect(stream, Collectors.toCollection(constructor));
    }

    private static <T> List<T> toList(final Stream<T> stream) {
        return toCollection(stream, ArrayList::new);
    }

    private static <K, V> Stream<Map.Entry<K, V>> entryStream(final Map<K, V> map) {
        return map.entrySet().stream();
    }

    private static <T> T collectStudents(final Collection<Student> students, final Collector<Student, ?, T> collector) {
        return collect(students.stream(), collector);
    }

    private static List<Group> getGroupsBy(final Collection<Student> students, final Comparator<Student> comparator) {
        return toList(entryStream(
                collectStudents(students, Collectors.groupingBy(Student::getGroup)))
                .map(entry -> new Group(entry.getKey(), sortedStudents(entry.getValue(), comparator)))
                .sorted(Comparator.comparing(Group::getName)));
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsBy(students, STUDENT_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsBy(students, Comparator.naturalOrder());
    }

    private static <T extends Collection<V>, K, V> Map<K, T> groupingBy(final Collection<Student> students,
                                                                        final Function<Student, K> keyFunction,
                                                                        final Function<Student, V> valueFunction,
                                                                        final Collector<V, ?, T> collector) {
        return collectStudents(students, Collectors.groupingBy(keyFunction, Collectors.mapping(valueFunction, collector)));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String orElseDefault(final Optional<String> optional) {
        return optional.orElse(DEFAULT_VALUE);
    }

    private static <T, K> Optional<K> getMaxAndMap(final Stream<T> stream,
                                                   final Comparator<T> comparator,
                                                   final Function<T, K> mapFunciton) {
        return stream.max(comparator).map(mapFunciton);
    }

    private static <T extends Collection<V>, K, V> Optional<K> getGroupingMaxAndMap(final Collection<Student> students,
                                                                                    final Function<Student, K> keyFunction,
                                                                                    final Function<Student, V> valueFunction,
                                                                                    final Collector<V, ?, T> collector,
                                                                                    final Comparator<Map.Entry<K, T>> comparator) {
        return getMaxAndMap(
                entryStream(groupingBy(students, keyFunction, valueFunction, collector)),
                comparator,
                Map.Entry::getKey);
    }

    private static <T extends Collection<String>> GroupName getLargestGroupBy(final Collection<Student> students,
                                                                              final Collector<String, ?, T> collector,
                                                                              final Comparator<Map.Entry<GroupName, T>> comparator) {
        return getGroupingMaxAndMap(students, Student::getGroup, Student::getFirstName, collector, comparator)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getLargestGroupBy(students, Collectors.toList(), GROUP_GREATER_COMPARATOR);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestGroupBy(students, Collectors.toSet(), GROUP_LOWER_COMPARATOR);
    }

    private static <T> List<T> mapToList(final Collection<Student> students, final Function<Student, T> mapFunction) {
        return toList(students.stream().map(mapFunction));
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    private static String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapToList(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return toCollection(getFirstNames(students).stream(), TreeSet::new);
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return orElseDefault(getMaxAndMap(students.stream(), Comparator.naturalOrder(), Student::getFirstName));
    }

    private static List<Student> sortedStudents(final Collection<Student> students, final Comparator<Student> comparator) {
        return sortedStudents(students.stream(), comparator);
    }

    private static List<Student> sortedStudents(final Stream<Student> stream, final Comparator<Student> comparator) {
        return toList(stream.sorted(comparator));
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortedStudents(students, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortedStudents(students, STUDENT_COMPARATOR);
    }

    private static List<Student> sortStudentsByName(final Stream<Student> stream) {
        return sortedStudents(stream, STUDENT_COMPARATOR);
    }

    private <T> List<Student> findStudentsBy(final Collection<Student> students, final Function<Student, T> getter, final T name) {
        return sortStudentsByName(students.stream().filter(student -> Objects.equals(name, getter.apply(student))));
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsBy(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsBy(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentsBy(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return collectStudents(findStudentsByGroup(students, group),
                Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    private static final Comparator<Map.Entry<String, Set<GroupName>>> popularNameComparator =
            Map.Entry.<String, Set<GroupName>>comparingByValue(Comparator.comparingInt(Set::size))
                    .thenComparing(Map.Entry.comparingByKey());

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return orElseDefault(getGroupingMaxAndMap(students, Student::getFirstName, Student::getGroup, Collectors.toSet(), popularNameComparator));
    }

    private static <T> List<T> getByIndices(final Collection<Student> students, final int[] indices, final Function<Student, T> getter) {
        return toList(Arrays.stream(indices).mapToObj(i -> mapToList(students, getter).get(i)));
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, StudentDB::getFullName);
    }
}

