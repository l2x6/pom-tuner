/*
 * Copyright (c) 2015 Maven Utilities Project
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.pom.tuner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.pom.tuner.model.Gav;
import org.l2x6.pom.tuner.model.Gavtc.Type;
import org.l2x6.pom.tuner.model.Gavtcf;

public class LocalMavenRepositoryTest {
    @Test
    void local() {
        Path root = Paths.get("target/test-classes/LocalMavenRepository").toAbsolutePath().normalize();

        MavenRepository repo = MavenRepository.local(root);
        List<Gavtcf> found = repo.gavtcfStream().collect(Collectors.toList());
        Gav pt490 = Gav.of("org.l2x6.pom-tuner:pom-tuner:4.9.0");
        Gav pt4100 = Gav.of("org.l2x6.pom-tuner:pom-tuner:4.10.0");
        Assertions.assertThat(found).containsExactlyInAnyOrder(
                Gav.of("org.l2x6.pom-tuner:pom-tuner-parent:4.9.0").toGavtc(Type.pom(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner-parent/4.9.0/pom-tuner-parent-4.9.0.pom")),
                pt490.toGavtc(Type.pom(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.9.0/pom-tuner-4.9.0.pom")),
                pt490.toGavtc(Type.jar(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.9.0/pom-tuner-4.9.0.jar")),
                pt490.toGavtc(Type.jar(), "sources")
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.9.0/pom-tuner-4.9.0-sources.jar")),

                Gav.of("org.l2x6.pom-tuner:pom-tuner-parent:4.10.0").toGavtc(Type.pom(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner-parent/4.10.0/pom-tuner-parent-4.10.0.pom")),
                pt4100.toGavtc(Type.pom(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.10.0/pom-tuner-4.10.0.pom")),
                pt4100.toGavtc(Type.jar(), null)
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.10.0/pom-tuner-4.10.0.jar")),
                pt4100.toGavtc(Type.jar(), "sources")
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.10.0/pom-tuner-4.10.0-sources.jar")),
                pt4100.toGavtc(Type.jar(), "javadoc")
                        .toGavtcf(root.resolve("org/l2x6/pom-tuner/pom-tuner/4.10.0/pom-tuner-4.10.0-javadoc.jar")));
    }
}
