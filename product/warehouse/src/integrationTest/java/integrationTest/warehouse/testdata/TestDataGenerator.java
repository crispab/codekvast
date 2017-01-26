package integrationTest.warehouse.testdata;

import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.warehouse.file_import.ImportDAO;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.Application;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.ImportContext;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.Invocation;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A simple reflection-based test data generator for inserting realistic data into the database when testing the query service.
 *
 * @author olle.hallin@crisp.se
 */
public class TestDataGenerator {

    private final ImportDAO importDao;

    private final List<ImportDescriptor.MethodDescriptor> methods = new ArrayList<>();

    @Inject
    public TestDataGenerator(ImportDAO importDao) {
        this.importDao = importDao;

        Class[] testClasses = {TestClass1.class, TestClass2.class};

        long localId = 0;
        for (Class testClass : testClasses) {
            Method[] declaredMethods = testClass.getDeclaredMethods();

            Arrays.sort(declaredMethods, Comparator.comparing(Method::getName));

            for (Method method : declaredMethods) {
                if (!method.getName().contains("jacoco")) {
                    methods.add(new ImportDescriptor.MethodDescriptor(localId, method));
                    localId += 1;
                }
            }
        }
    }

    public int numMethods() {
        return methods.size();
    }

    public ImportDescriptor.MethodDescriptor getMethod(int index) {
        return methods.get(index);
    }

    @Transactional(rollbackFor = Exception.class)
    public void simulateFileImport(ImportDescriptor descriptor) {
        ImportContext context = new ImportContext();
        importApplications(descriptor, context);
        importMethods(descriptor, context);
        importJvms(descriptor, context);
        importInvocations(descriptor, context);
    }

    private void importApplications(ImportDescriptor descriptor, ImportContext context) {
        for (String app : descriptor.getApps()) {
            String[] strings = app.split("\\s+");

            importDao.saveApplication(Application.builder()
                                                 .localId(Long.valueOf(strings[0]))
                                                 .name(strings[1])
                                                 .version(strings[2])
                                                 .createdAtMillis(descriptor.getNow())
                                                 .build(),
                                      context);

        }
    }

    private void importMethods(ImportDescriptor descriptor, ImportContext context) {
        for (ImportDescriptor.MethodDescriptor methodDescriptor : descriptor.getMethods()) {
            Method method = methodDescriptor.getMethod();
            String signature = method.toString();
            String packageName = method.getDeclaringClass().getPackage().getName();
            String parameterTypes = prettyPrint(method.getParameterTypes());
            String exceptionTypes = prettyPrint(method.getExceptionTypes());
            String returnType = method.getReturnType().toString();

            importDao.saveMethod(ImportDAO.Method.builder()
                                                 .createdAtMillis(descriptor.getNow())
                                                 .declaringType(method.getDeclaringClass().getCanonicalName())
                                                 .exceptionTypes(exceptionTypes)
                                                 .localId(methodDescriptor.getLocalId())
                                                 .methodName(method.getName())
                                                 .modifiers(method.getModifiers() + "")
                                                 .packageName(packageName)
                                                 .parameterTypes(parameterTypes)
                                                 .returnType(returnType)
                                                 .signature(signature)
                                                 .visibility("public")
                                                 .build(),
                                 context);
        }
    }

    private void importJvms(ImportDescriptor descriptor, ImportContext context) {
        for (ImportDescriptor.JvmDataPair pair : descriptor.getJvms()) {
            importDao.saveJvm(pair.getJvm(), pair.getJvmData(), context);
        }
    }

    private void importInvocations(ImportDescriptor descriptor, ImportContext context) {
        for (Invocation invocation : descriptor.getInvocations()) {
            importDao.saveInvocation(invocation, context);
        }
    }

    private String prettyPrint(Class<?>[] classes) {
        return Arrays.asList(classes).toString().replace("[", "").replace("]", "").replace("class ", "");
    }

}
