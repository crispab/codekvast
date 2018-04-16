package io.codekvast.admin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = CodekvastAdminApplication.class,
    properties = {"server.port=0", "management.server.port=0"})
public class CodekvastAdminApplicationTest {

    @Test
    public void contextLoads() {
    }

}
