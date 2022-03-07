package com.marginallyclever.makelangelo.makeArt.io.vector;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.marginallyclever.makelangelo.makeArt.io.vector.LoadHelper.loadAndTestFiles;
import static com.marginallyclever.makelangelo.makeArt.io.vector.LoadHelper.readFile;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

public class LoadGCodeTest {

    @BeforeEach
    public void beforeEach() {
        PreferencesHelper.start();
        Translator.start();
    }

    @Test
    public void canLoad() {
        // given
        TurtleLoader loader = new LoadGCode();

        // then
        assertTrue(loader.canLoad("file.gcode"));
        assertTrue(loader.canLoad("file.GCode"));
        assertFalse(loader.canLoad("file.txt"));
    }

    @Test
    public void throwExceptionWhenStreamIsNull() {
        // given
        TurtleLoader loader = new LoadGCode();

        // then
        assertThrows(NullPointerException.class, () -> {
            loader.load(LoadGCodeTest.class.getResourceAsStream("/doesNotExist"));
        }, "Input stream is null");
    }

    @TestFactory
    public Stream<DynamicTest> testAllFiles() {
        return loadAndTestFiles(of("multi_shapes_ignatus2.gcode",
                "multi_shapes_path-circle-line-rect.gcode"),
                "/gcode",
                this::verifyLoadGCode);
    }

    private void verifyLoadGCode(String filenameToTest, String fileExpected) {
        try {

            // given
            TurtleLoader loader = new LoadGCode();

            // when
            Turtle turtle = loader.load(LoadGCodeTest.class.getResourceAsStream(filenameToTest));

            // then
            assertNotNull(turtle);
            assertNotNull(turtle.history);
            assertEquals(readFile(fileExpected), turtle.history.toString());
        } catch( Exception e) {
            fail(e);
        }
    }
}