package br.com.tresvtintas.mobile.feature.quote;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintConfiguration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class MaterialQuoteTintOptions {
    private MaterialQuoteTintOptions() {
    }

    static List<String> sources(List<MaterialQuoteTintConfiguration> values) {
        return distinct(values, MaterialQuoteTintConfiguration::sourceSystem);
    }

    static List<String> lines(
            List<MaterialQuoteTintConfiguration> values,
            String source) {
        return distinct(
                values.stream()
                        .filter(value -> value.sourceSystem().equals(source))
                        .collect(Collectors.toList()),
                MaterialQuoteTintConfiguration::lineName);
    }

    static List<String> finishes(
            List<MaterialQuoteTintConfiguration> values,
            String source,
            String line) {
        return distinct(
                values.stream()
                        .filter(value -> value.sourceSystem().equals(source)
                                && value.lineName().equals(line))
                        .collect(Collectors.toList()),
                MaterialQuoteTintConfiguration::finishName);
    }

    static List<String> packages(
            List<MaterialQuoteTintConfiguration> values,
            String source,
            String line,
            String finish) {
        return distinct(
                values.stream()
                        .filter(value -> value.sourceSystem().equals(source)
                                && value.lineName().equals(line)
                                && value.finishName().equals(finish))
                        .collect(Collectors.toList()),
                MaterialQuoteTintConfiguration::packageName);
    }

    static MaterialQuoteTintConfiguration configuration(
            List<MaterialQuoteTintConfiguration> values,
            String source,
            String line,
            String finish,
            String packageName) {
        return values.stream()
                .filter(value -> value.sourceSystem().equals(source)
                        && value.lineName().equals(line)
                        && value.finishName().equals(finish)
                        && value.packageName().equals(packageName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected tint configuration is unavailable."));
    }

    private static List<String> distinct(
            List<MaterialQuoteTintConfiguration> values,
            Function<MaterialQuoteTintConfiguration, String> mapper) {
        return values.stream()
                .map(mapper)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
