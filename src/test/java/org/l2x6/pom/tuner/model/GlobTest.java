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
package org.l2x6.pom.tuner.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlobTest {

    @Test
    public void question() {
        assertPattern("som?.c", "some.c", true);
        assertPattern("som?.c", "soma.c", true);
        assertPattern("som?.c", "som.c", false);
        assertPattern("som?.c", "sone.c", false);
    }

    @Test
    public void braces() {

        // word choice
        assertPattern("*.{py,js,html}", "foo.py", true);
        assertPattern("*.{py,js,html}", "foo.js", true);
        assertPattern("*.{py,js,html}", "foo.html", true);
        assertPattern("*.{py,js,html}", "foo.bar", false);

        // single choice
        assertPattern("*.{py}", "foo.{py}", true);
        assertPattern("*.{py}", "foo.py", false);
        assertPattern("*.{py}", "foo.bar", false);

        // empty choice
        assertPattern("*.{}", "foo.{}", true);
        assertPattern("*.{}", "foo.", false);
        assertPattern("*.{}", "foo.bar", false);

        // choice with empty word
        assertPattern("a{b,c,}.d", "ab.d", true);
        assertPattern("a{b,c,}.d", "ac.d", true);
        assertPattern("a{b,c,}.d", "a.d", true);
        assertPattern("a{b,c,}.d", "ad.d", false);
        //
        // choice with empty words
        assertPattern("a{,b,,c,}.d", "ab.d", true);
        assertPattern("a{,b,,c,}.d", "ac.d", true);
        assertPattern("a{,b,,c,}.d", "a.d", true);
        assertPattern("a{,b,,c,}.d", "ad.d", false);

        // no closing brace
        assertPattern("{.f", "{.f", true);
        assertPattern(".f", "{.f", false);

        // nested braces
        assertPattern("{word,{also},this}.g", "word.g", true);
        assertPattern("{word,{also},this}.g", "{also}.g", true);
        assertPattern("{word,{also},this}.g", "this.g", true);
        assertPattern("{word,{also},this}.g", "foo.g", false);

        // nested braces, adjacent at start
        assertPattern("{{a,b},c}.d", "a.d", true);
        assertPattern("{{a,b},c}.d", "a.d", true);
        assertPattern("{{a,b},c}.d", "c.d", true);
        assertPattern("{{a,b},c}.d", "d.d", false);

        // nested braces, adjacent at end
        assertPattern("{a,{b,c}}.d", "a.d", true);
        assertPattern("{a,{b,c}}.d", "b.d", true);
        assertPattern("{a,{b,c}}.d", "c.d", true);
        assertPattern("{a,{b,c}}.d", "d.d", false);

        // closing inside beginning
        //[{},b}.h]
        //closing=inside
        //
        // opening inside beginning
        //[{{,b,c{d}.i]
        //unmatched=true
        //
        // escaped comma
        //[{a\,b,cd}.txt]
        //comma=yes
        //
        // escaped closing brace
        //[{e,\},f}.txt]
        //closing=yes
        //
        // escaped backslash
        //[{g,\\,i}.txt]
        //backslash=yes
        //
        // patterns nested in braces
        //[{some,a{*c,b}[ef]}.j]
        //patterns=nested
        //
        // numeric braces
        //[{3..120}]
        //number=true
        //
        // alphabetical
        //[{aardvark..antelope}]
        //words=a
    }

    @Test
    public void brackets() {
        // Character choice
        assertPattern("[ab].a", "a.a", true);
        assertPattern("[ab].a", "b.a", true);
        assertPattern("[ab].a", "c.a", false);

        // Negative character choice
        assertPattern("[!ab].a", "a.a", !true);
        assertPattern("[!ab].a", "b.a", !true);
        assertPattern("[!ab].a", "c.a", !false);

        // Character range
        assertPattern("[a-c].a", "a.a", true);
        assertPattern("[a-c].a", "b.a", true);
        assertPattern("[a-c].a", "c.a", true);
        assertPattern("[a-c].a", "d.a", false);

        // Negative character range
        assertPattern("[!a-c].a", "a.a", !true);
        assertPattern("[!a-c].a", "b.a", !true);
        assertPattern("[!a-c].a", "c.a", !true);
        assertPattern("[!a-c].a", "d.a", !false);

        // Range and choice
        assertPattern("[a-cef].a", "a.a", true);
        assertPattern("[a-cef].a", "b.a", true);
        assertPattern("[a-cef].a", "c.a", true);
        assertPattern("[a-cef].a", "d.a", false);
        assertPattern("[a-cef].a", "e.a", true);
        assertPattern("[a-cef].a", "f.a", true);

        // Choice with dash
        assertPattern("[-ab].a", "a.a", true);
        assertPattern("[-ab].a", "b.a", true);
        assertPattern("[-ab].a", "-.a", true);
        assertPattern("[-ab].a", "c.a", false);

        // Close bracket inside
        assertPattern("[\\]ab].a", "a.a", true);
        assertPattern("[\\]-ab].a", "b.a", true);
        assertPattern("[\\]-ab].a", "].a", true);
        assertPattern("[\\]-ab].a", "c.a", false);

        // Close bracket outside
        assertPattern("[ab]].a", "a].a", true);
        assertPattern("[ab]].a", "b].a", true);
        assertPattern("[ab]].a", "c].a", false);

        // Negative close bracket inside
        assertPattern("[!\\]ab].a", "a.a", !true);
        assertPattern("[!\\]-ab].a", "b.a", !true);
        assertPattern("[!\\]-ab].a", "].a", !true);
        assertPattern("[!\\]-ab].a", "c.a", !false);

        // Negative close bracket outside
        assertPattern("[!ab]].a", "a].a", !true);
        assertPattern("[!ab]].a", "b].a", !true);
        assertPattern("[!ab]].a", "c].a", !false);

        // Slash inside brackets
        assertPattern("ab[e/].a", "ab[e/].a", true);
        assertPattern("ab[e/].a", "ab/.a", false);
        assertPattern("ab[e/].a", "abe.a", false);

        // Slash after an half-open bracket
        assertPattern("ab[/.a", "ab[/.a", true);
        assertPattern("ab[/.a", "ab/.a", false);
        assertPattern("ab[/.a", "ab[/.a", true);

    }

    @Test
    public void starStar() {
        assertPattern("a**z.c", "az.c", true);
        assertPattern("a**z.c", "ab/c/z.c", true);
        assertPattern("a**z.c", "a/c/d/z.c", true);
        assertPattern("a**z.c", "ab.c", false);

        assertPattern("b/a**z.c", "b/az.c", true);
        assertPattern("b/a**z.c", "b/ab/c/z.c", true);
        assertPattern("b/a**z.c", "b/a/c/d/z.c", true);
        assertPattern("b/a**z.c", "b/ab.c", false);

        assertPattern("a**/z.c", "a/z.c", true);
        assertPattern("a**/z.c", "ab/c/z.c", true);
        assertPattern("a**/z.c", "a/c/d/z.c", true);
        assertPattern("a**/z.c", "a/b.c", false);

        assertPattern("b/**/z.c", "b/a/z.c", true);
        assertPattern("b/**/z.c", "b/ab/c/z.c", true);
        assertPattern("b/**/z.c", "b/a/c/d/z.c", true);
        assertPattern("b/**/z.c", "b/a/b.c", false);
    }

    @Test
    void star() {
        assertPattern("*/foo/Bar", "com/foo/Bar", true);
        assertPattern("*/foo/Bar", "com/foo/Baz", false);
        assertPattern("com/*/Bar", "com/foo/Bar", true);
        assertPattern("org/*/Bar", "com/foo/Bar", false);
        assertPattern("com/foo/*", "com/foo/Bar", true);
        assertPattern("com/bar/*", "com/foo/Bar", false);

        assertPattern("com/**", "com/foo/Bar", true);
        assertPattern("com/**", "com/bar/Bar", true);

        assertPattern("com/**/Bar", "com/foo/Bar", true);
        assertPattern("com/**/Bar", "com/foo/baz/Bar", true);

        assertPattern("*", "com/foo/Bar", false);
        assertPattern("*", "Bar", true);

        assertPattern("a*e.c", "abbe.c", true);
        assertPattern("a*e.c", "ae.c", true);
        assertPattern("a*e.c", "aef.c", false);
        assertPattern("com/*", "com/Bar", true);
        assertPattern("org/*", "com/Bar", false);

    }

    static void assertPattern(String pattern, String path, boolean expected) {
        Assertions.assertThat(new Glob(pattern).matches(path))
                .withFailMessage(() -> "Pattern " + pattern + " compiled to regex " + new Glob(pattern).regex + " should"
                        + (expected ? "" : " not") + " match path " + path)
                .isEqualTo(expected);
    }
}
