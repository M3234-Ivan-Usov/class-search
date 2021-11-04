package ru.ifmo.rain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassFinderTest {
    private void matchTest(String pattern, Set<String> matches, Set<String> notMatches) {
        Set<String> names = new HashSet<>(matches);
        names.addAll(notMatches);
        assertEquals(matches, new ClassFinder(names).search(pattern));
    }

    @Test
    public void simpleTest() {
        matchTest("FooBar",
                Set.of("FooBar", "FooBarBaz", "FoolBars"),
                Set.of("FoBar", "FooBa")
        );
    }

    @Test
    public void lowerTest1() {
        matchTest("fb",
                Set.of("FooBarBaz", "FooBar", "FBK", "Fbk", "FaFeBe", "CatFtBa", "AbsFba", "efBt"),
                Set.of("Fiber", "TauFeb", "AtFr")
        );
    }

    @Test
    public void lowerTest2() {
        matchTest("fbk",
                Set.of("FaceBooK", "FaceBk", "FBK", "xfbkx", "FBke", "FaceBraceKFC"),
                Set.of("FiberK", "TauFebk", "FBekara", "NoFNoBYk")
        );
    }

    @Test
    public void singleWildcardTest1() {
        matchTest("F*B",
                Set.of("FooBar", "FB", "FxCxbxBx", "TfFooBarsCat", "AFbBaFt"),
                Set.of("Fb", "AeFbT", "AeFbFv")
        );
    }

    @Test
    public void singleWildcardTest2() {
        matchTest("F*lCat",
                Set.of("FoolCat", "FlCatT", "FlCatlCatX", "FallsCat", "AbsFailureCat"),
                Set.of("FoolCaT", "foolCat", "FarCat")
        );
    }

    @Test
    public void doubleWildCardTest() {
        matchTest("F*b*k",
                Set.of("Facebook", "NotFaCebOok", "FnobPayback"),
                Set.of("FaceBook", "NoFaCebOot", "reFBlock")
        );
    }

    @Test
    public void withPackageTest() {
        matchTest("FB",
                Set.of("ru.ifmo.rain.FooBar", "ru.FeeBoo", "pack.name.SomeFoolBars"),
                Set.of("ru.ifmo.rain.NoMatch", "in.FooBar.middle", "Foo.Bar")
        );
    }

    @Test
    public void walkTest() throws URISyntaxException, IOException {
        URL rootDir = Thread.currentThread().getContextClassLoader().getResource("walktest");
        ClassFinder searcher = new ClassFinder(Path.of(rootDir.toURI()));
        Set<String> result = searcher.search("FooBar");
        assertEquals(Set.of("FooBar", "depth1.FooBar", "depth1.depth22.SameFooBar", "YetAnotherFoolBar"), result);
    }
}