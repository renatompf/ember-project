package core.di;

import core.di.mock.AnnotatedClass;
import core.di.mock.NonAnnotatedClass;
import core.di.mock.annotations.TestAnnotation;
import io.github.renatompf.ember.core.di.ClassScanner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClassScannerTest {

    @Test
    public void testFindAnnotatedClasses_returnsAnnotatedClass() throws Exception {
        ClassScanner scanner = new ClassScanner();
        List<Class<?>> result = scanner.findAnnotatedClasses("core.di", TestAnnotation.class);

        assertNotNull(result);
        assertTrue(result.contains(AnnotatedClass.class));
        assertFalse(result.contains(NonAnnotatedClass.class));
    }

    @Test
    public void testFindAnnotatedClasses_returnsEmptyWhenNoMatch() throws Exception {
        ClassScanner scanner = new ClassScanner();
        List<Class<?>> result = scanner.findAnnotatedClasses("core.di", Deprecated.class); // Assuming none are deprecated

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAnnotatedClasses_invalidPackage_returnsEmpty() throws Exception {
        ClassScanner scanner = new ClassScanner();
        List<Class<?>> result = scanner.findAnnotatedClasses("non.existent", TestAnnotation.class);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}