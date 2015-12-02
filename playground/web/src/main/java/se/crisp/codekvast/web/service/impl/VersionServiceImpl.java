package se.crisp.codekvast.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.web.service.VersionService;

import javax.annotation.PostConstruct;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class VersionServiceImpl implements VersionService {

    @Autowired
    @Value("${info.build.gradle.name}")
    private String gradleName;

    @Autowired
    @Value("${info.build.gradle.description}")
    private String gradleDescription;

    @Autowired
    @Value("${info.build.gradle.version}")
    private String gradleVersion;

    @Autowired
    @Value("${info.build.git.id}")
    private String gitId;

    @Autowired
    @Value("${info.build.git.committer}")
    private String gitCommitter;

    @Autowired
    @Value("${info.build.git.message}")
    private String gitMessage;

    @Autowired
    @Value("${info.build.git.time}")
    private String gitTime;

    @Override
    public String getFullBuildVersion() {
        return String.format("v%s.%s @ %s", gradleVersion, gitId, gitTime);
    }

    @PostConstruct
    public void logStartup() {
        log.info("Codekvast Web {} started", getFullBuildVersion());
    }
}
