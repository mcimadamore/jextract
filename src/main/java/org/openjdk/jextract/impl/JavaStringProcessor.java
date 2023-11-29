package org.openjdk.jextract.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaStringProcessor {

    public static final StringTemplate.Processor<String, RuntimeException> JAVA =
            st -> TemplateUtils.lines(st)
                    .flatMap(TemplateUtils::indent)
                    .collect(TemplateUtils.COLLECTOR)
                    .interpolate();
    public class TemplateUtils {
        public static final StringTemplateCollector COLLECTOR = new StringTemplateCollector();

        static public class StringTemplateCollector implements Collector<StringTemplate, List<StringTemplate>, StringTemplate> {
            @Override
            public Supplier<List<StringTemplate>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<StringTemplate>, StringTemplate> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<StringTemplate>> combiner() {
                return (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                };
            }

            @Override
            public Function<List<StringTemplate>, StringTemplate> finisher() {
                return StringTemplate::combine;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        }

        public static Stream<StringTemplate> lines(StringTemplate stringTemplate) {
            StringBuilder sb = new StringBuilder();
            List<String> fragments = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            Iterator<Object> valueIter = stringTemplate.values().iterator();
            Stream.Builder<StringTemplate> builder = Stream.builder();

            for (String fragment : stringTemplate.fragments()) {
                fragment.codePoints()
                        .forEach(cp -> {
                            sb.appendCodePoint(cp);

                            if (cp == '\n') {
                                fragments.add(sb.toString());
                                sb.setLength(0);
                                builder.add(StringTemplate.of(fragments, values));
                                fragments.clear();
                                values.clear();
                            }
                        });

                fragments.add(sb.toString());
                sb.setLength(0);

                if (valueIter.hasNext()) {
                    values.add(valueIter.next());
                }
            }

            builder.add(StringTemplate.of(fragments, values));

            return builder.build();
        }

        private static Stream<StringTemplate> indent(StringTemplate stringTemplate) {
            List<Object> newValues = new ArrayList<>();
            for (int i = 0 ; i < stringTemplate.values().size() ; i++) {
                String prevFragment = stringTemplate.fragments().get(i);
                Object currValue = stringTemplate.values().get(i);
                if (currValue instanceof String str) {
                    String indentString = " ".repeat(prevFragment.length());
                    newValues.add(str.lines().collect(Collectors.joining(STR."\n\{indentString}")));
                } else {
                    newValues.add(currValue);
                }
            }
            return Stream.of(StringTemplate.of(stringTemplate.fragments(), newValues));
        }

    }
}
