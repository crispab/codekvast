package integrationTest.warehouse.testdata;

import io.codekvast.warehouse.file_import.impl.ImportDAO;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple reflection-based test data generator for inserting realistic data into the database when testing the query service.
 *
 * @author olle.hallin@crisp.se
 */
public class TestDataGenerator {

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ImportDAO importDao;

    @Inject
    public TestDataGenerator(ImportDAO importDao) {
        this.importDao = importDao;

        Class[] testClasses = {TestClass1.class, TestClass2.class};

        for (Class testClass : testClasses) {
            Method[] declaredMethods = testClass.getDeclaredMethods();

            Arrays.sort(declaredMethods, Comparator.comparing(Method::getName));

            for (Method method : declaredMethods) {
                if (!method.getName().contains("jacoco")) {
                    // TODO: implement
                }
            }
        }
    }
}
