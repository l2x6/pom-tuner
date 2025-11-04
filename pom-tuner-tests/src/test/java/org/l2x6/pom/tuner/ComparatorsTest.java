package org.l2x6.pom.tuner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComparatorsTest {
    @Test
    void after() {

        assertComparator(
                Comparators.after("b"),
                Arrays.asList("a", "b", "c"),
                "b",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.after("b"),
                Arrays.asList("a", "b", "c"),
                "c",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.after("b"),
                Arrays.asList("a", "b", "c"),
                "a",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.after("b"),
                Arrays.asList("a", "b", "c"),
                "new",
                Arrays.asList("a", "b", "new", "c"));

        assertComparator(
                Comparators.after("bad"),
                Arrays.asList("a", "b", "c"),
                "new",
                Arrays.asList("a", "b", "c", "new"));

        assertComparator(
                Comparators.after("bad"),
                Arrays.asList(),
                "new",
                Arrays.asList("new"));

    }

    @Test
    void before() {

        assertComparator(
                Comparators.before("b"),
                Arrays.asList("a", "b", "c"),
                "b",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.before("b"),
                Arrays.asList("a", "b", "c"),
                "c",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.before("b"),
                Arrays.asList("a", "b", "c"),
                "a",
                Arrays.asList("a", "b", "c"));

        assertComparator(
                Comparators.before("b"),
                Arrays.asList("a", "b", "c"),
                "new",
                Arrays.asList("a", "new", "b", "c"));

        assertComparator(
                Comparators.before("bad"),
                Arrays.asList("a", "b", "c"),
                "new",
                Arrays.asList("a", "b", "c", "new"));

        assertComparator(
                Comparators.before("bad"),
                Arrays.asList(),
                "new",
                Arrays.asList("new"));

    }

    static void assertComparator(
            Comparator<String> comparator,
            List<String> input,
            String newElement,
            List<String> expected) {
        final List<String> newList = insert(input, newElement, comparator);
        Assertions.assertThat(newList).isEqualTo(expected);
    }

    private static <T> List<T> insert(List<T> input, T newElement, Comparator<T> comparator) {
        final List<T> newList = new ArrayList<>(input);
        OptionalInt insertPos = OptionalInt.empty();
        for (int i = 0; i < newList.size(); i++) {
            T child = newList.get(i);
            int comparison = comparator.compare(newElement, child);
            if (comparison == 0) {
                /* the given child is available, no need to add it */
                return newList;
            }
            if (!insertPos.isPresent() && comparison < 0) {
                insertPos = OptionalInt.of(i);
            }
        }
        if (insertPos.isPresent()) {
            newList.add(insertPos.getAsInt(), newElement);
        } else {
            newList.add(newElement);
        }
        return newList;
    }
}
